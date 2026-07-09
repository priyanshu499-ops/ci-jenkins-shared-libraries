package opstree.common

import opstree.common.argocd_deployment

import opstree.common.*

def deployment_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_argocd_deployment == 'true') {
        eks_deployment(step_params)
    }
  else {
        logger.logger('msg':"${step_params.perform_argocd_deployment} No valid option selected for Deplying application via Argocd. Please mention correct values.", 'level':'WARN')
  }
}


def eks_deployment(Map step_params) {
    logger = new logger()

    logger.logger('msg':'Performing ArgoCD Deployment Step', 'level':'INFO')

    app_name               = "${step_params.app_name}"
    app_type               = "${step_params.app_type}"
    app_env                = "${step_params.app_env}"
    repo_url               = "${step_params.repo_url}"
    argocd_credential_id   = "${step_params.argocd_credential_id}"
    argocd_server_env_name = "${step_params.argocd_server_env_name}"
    argocd_server_url      = env[argocd_server_env_name]
    prune_post_deployment  = "${step_params.prune_post_deployment}"
    helm_chart_path        = "${step_params.helm_chart_path}"
    repo_branch            = "${step_params.repo_branch}"
    dry_run                = "${step_params.dry_run}"
    destination_namespace  = step_params.destination_namespace?.toString()?.trim() ?: app_env ?: "default"
    values_file_path       = "${step_params.values_file_path}"

    // values repo: separate repo for $values ref, or fall back to same repo_url
    values_repo_url        = step_params.values_repo_url?.toString()?.trim() ?: repo_url
    values_repo_revision   = step_params.values_repo_revision?.toString()?.trim() ?: repo_branch

    // argocd_project: explicit override, or built from app_type-app_env, or just app_env
    def raw_argocd_project = step_params.argocd_project?.toString()?.trim()
    argocd_project         = (raw_argocd_project && raw_argocd_project != 'null')
                                ? raw_argocd_project
                                : (app_type && app_type != 'null' && app_type.trim())
                                    ? "${app_type}-${app_env}"
                                    : "${app_env}"

    // eks_api_endpoint: from env var if given, else default to in-cluster
    def eks_api_endpoint_env_name = step_params.eks_api_endpoint_env_name?.toString()?.trim() ?: ""
    eks_api_endpoint = (eks_api_endpoint_env_name && env[eks_api_endpoint_env_name])
                         ? env[eks_api_endpoint_env_name]
                         : "https://kubernetes.default.svc"

    def finalTag = params.custom_image_tag?.trim() ?
               params.custom_image_tag.trim() :
               params.image_tag

    def release_name = app_name

    withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {
        logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
        sh "argocd login ${argocd_server_url} --username admin --password $PASSWORD --insecure --grpc-web"

        logger.logger('msg':'Creating or updating ArgoCD application...', 'level':'INFO')
        def applicationYaml

        if (finalTag) {
            echo "dry run : ${dry_run}"

            if (dry_run.toBoolean() || dry_run == "true") {

                applicationYaml = """
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${app_name}-${app_env}
  namespace: argocd
  annotations:
    argocd.argoproj.io/compare-options: ServerSideDiff=true
spec:
  project: ${argocd_project}

  destination:
    server: ${eks_api_endpoint}
    namespace: ${destination_namespace}

  sources:
    - repoURL: ${repo_url}
      targetRevision: ${repo_branch}
      path: ${helm_chart_path}
      helm:
        releaseName: ${release_name}
        valueFiles:
          - \$values/${values_file_path}
        parameters:
          - name: image.tag
            value: ${finalTag}
    - repoURL: ${values_repo_url}
      targetRevision: ${values_repo_revision}
      ref: values
"""
            }
            else {

                applicationYaml = """
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${app_name}-${app_env}
  namespace: argocd
  annotations:
    argocd.argoproj.io/compare-options: ServerSideDiff=true
spec:
  project: ${argocd_project}
  syncPolicy:
    automated:
      prune: false
      selfHeal: true

  destination:
    server: ${eks_api_endpoint}
    namespace: ${destination_namespace}

  sources:
    - repoURL: ${repo_url}
      targetRevision: ${repo_branch}
      path: ${helm_chart_path}
      helm:
        releaseName: ${release_name}
        valueFiles:
          - \$values/${values_file_path}
        parameters:
          - name: image.tag
            value: ${finalTag}
    - repoURL: ${values_repo_url}
      targetRevision: ${values_repo_revision}
      ref: values
"""
            }

            writeFile file: "application-${app_name}-${app_env}.yaml", text: applicationYaml

            sh "cat application-${app_name}-${app_env}.yaml && argocd app create -f application-${app_name}-${app_env}.yaml --upsert --grpc-web --validate=false"

            sleep(20)

            try {

                if (dry_run.toBoolean()) {

                    logger.logger('msg':'Skipping Syncing of application as Dry run is enabled', 'level':'WARN')
                    sh "argocd app diff ${app_name}-${app_env} --grpc-web"
                }
            } catch (Exception e) {

                echo "Changes detected. Please review and decide. "
                return
            }

            if (!dry_run.toBoolean()) {

                if (prune_post_deployment == "true") {
                    sh "argocd app sync ${app_name}-${app_env} --grpc-web"
                } else {
                    sh "argocd app sync ${app_name}-${app_env} --grpc-web"
                }
            }
        }
        else
        {
            sh """
            argocd app create $app_name \
                --repo $repo_url \
                --path $helm_chart_path \
                --values ${values_file_path} \
                --dest-server ${eks_api_endpoint} \
                --revision ${repo_branch} \
                --upsert

            sleep 10

            if [ "$prune_post_deployment" = "true" ]; then
               argocd app sync ${app_name}-${app_env}
            else
               argocd app sync ${app_name}-${app_env}
            fi
        """
        }

        logger.logger('msg':'Created or updated ArgoCD application...', 'level':'INFO')
        logger.logger('msg':'Checking Application Health check. Please wait for sometime...', 'level':'INFO')
        sh """
                MAX_RETRIES=10
                RETRY_INTERVAL=30 # in seconds
                RETRIES=0

                while [ \$RETRIES -lt \$MAX_RETRIES ]; do
                    APP_STATUS=\$(argocd app get ${app_name}-${app_env} --output json | jq -r '.status.health.status')

                    echo "Application health status: \$APP_STATUS"

                    if [ "\$APP_STATUS" = "Healthy" ]; then
                        echo "Application is Healthy."
                        exit 0
                    else
                        echo "Application is not Healthy yet. Retrying in \${RETRY_INTERVAL} seconds..."
                        sleep \${RETRY_INTERVAL}
                        RETRIES=\$((RETRIES + 1))
                    fi
                done

                echo "Application did not become healthy within the expected time."
                exit 1
                """

        logger.logger('msg':'Application Deployed successfully', 'level':'INFO')
    }
}
