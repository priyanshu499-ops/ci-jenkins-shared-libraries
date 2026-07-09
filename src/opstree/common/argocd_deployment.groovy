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
    parser = new parser()

    logger.logger('msg':'Performing Docker Build Step', 'level':'INFO')

    app_name = "${step_params.app_name}"
    app_type = "${step_params.app_type}"
    app_env = "${step_params.app_env}"
    repo_url = "${step_params.repo_url}"
    deployment_strategy = "${step_params.deployment_strategy}"
    argocd_credential_id = "${step_params.argocd_credential_id}"
    argocd_server_env_name = "${step_params.argocd_server_env_name}"
    argocd_server_url = env[argocd_server_env_name]
    eks_api_endpoint_env_name = "${step_params.eks_api_endpoint_env_name}"
    prune_post_deployment = "${step_params.prune_post_deployment}"
    eks_api_endpoint = env[eks_api_endpoint_env_name]
    helm_chart_path = "${step_params.helm_chart_path}"
    source_code_path = "${step_params.source_code_path}"
    prune_post_deployment = "${step_params.prune_post_deployment}"
    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    repo_dir = repo_dir + source_code_path
    repo_branch = "${step_params.repo_branch}"
    double_source = "${step_params.double_source}"
    values_repo_url = "${step_params.values_repo_url}"
    values_repo_revision = "${step_params.values_repo_revision}"
    destination_namespace = "${step_params.destination_namespace}"
    dry_run = "${step_params.dry_run}"
    argocd_project = "${app_type}-${app_env}"
    values_file_path = "helm_chart/${app_type}/${app_env}/${app_name}/${app_name}.yaml"

    release_name = "${app_name}"
    chart_version = "${step_params.chart_version}"

    if (app_name == "issuing-dolphinscheduler" && app_env == "uatpti") {
        release_name = "dolphinscheduler"
    }
    else if (app_name == "issuing-dolphinscheduler" && app_env == "uat") {
        release_name = "dolphinscheduler"
    }
    else if (app_name == "issuing-dolphinscheduler" && app_env == "uatmeadr") {
        release_name = "dolphinscheduler"
    }
    else if (app_name == "issuing-etl-engine" && app_env == "uatmeadr") {
        release_name = "cplus-etl-engine"
    }
    else if (app_name == "issuing-cplus-helpdesk" && app_env == "uatmeadr") {
        release_name = "cplus-helpdesk"
    }
    else if (app_name == "issuing-cplus-helpdesk-dashboard" && app_env == "uatmeadr") {
        release_name = "cplus-helpdesk-dashboard"
    }
    else if (app_name == "issuing-cplus-securenow-dashboard" && app_env == "uat") {
        release_name = "cplus-securenow-dashboard"
    }
    else if (app_name == "acquiring-etl-engine" && app_env == "enbduat") {
        release_name = "cplus-etl-engine"
    }
    else if (app_name == "acquiring-etl-engine" && app_env == "bureauapacuat") {
        release_name = "cplus-etl-engine"
    }
    else if (app_name == "issuing-cplus-etl-engine" && app_env == "uat") {
        release_name = "issuing-etl-engine"
    }
    else if (app_name == "acquiring-helpdesk" && app_env == "enbduat") {
        release_name = "cplus-helpdesk"
    }
    else if (app_name == "acquiring-helpdesk-dashboard" && app_env == "enbduat") {
        release_name = "cplus-helpdesk-dashboard"
    }
    else if (app_name == "issuing-helpdesk" && app_env == "uat") {
        release_name = "cplus-helpdesk"
    }
    else if (app_name == "issuing-helpdesk-dashboard" && app_env == "uat") {
        release_name = "cplus-helpdesk-dashboard"
    }
    else if (app_name == "acquiring-woohoo-delivery" && app_env == "enbduat") {
        release_name = "woohoo-delivery"
    }
    else if (app_name == "acquiring-woohoo-kms" && app_env == "enbduat") {
        release_name = "woohoo-kms"
    }
    else if (app_name == "acquiring-woohoo-delivery" && app_env == "bureauapacuat") {
        release_name = "woohoo-delivery"
    }
    else if (app_name == "issuing-woohoo-delivery" && app_env == "uat") {
        release_name = "woohoo-delivery"
    }
    else if (app_name == "issuing-woohoo-kms" && app_env == "uat") {
        release_name = "woohoo-kms"
    }
    else if (app_name == "issuing-woohoo-delivery" && app_env == "uatmeadr") {
        release_name = "woohoo-delivery"
    }
    else if (app_name == "issuing-woohoo-kms" && app_env == "uatmeadr") {
        release_name = "woohoo-kms"
    }
    else if (app_name == "acquiring-woohoo-kms" && app_env == "bureauapacuat") {
        release_name = "woohoo-kms"
    }
    else if (app_name == "acquiring-redis" && app_env == "enbduat") {
        release_name = "redisnew"
    }
    // else if (app_name == "acquiring-apache-pulsar" && app_env == "enbduat") {
    //     release_name = "acquiring-apache-pulsar"
    // }
    else if (app_name == "acquiring-dolphinscheduler" && app_env == "bureaumeauat") {
        release_name = "dolphinscheduler"
    }
    else if (app_name == "acquiring-dolphinscheduler" && app_env == "enbduat") {
        release_name = "dolphinscheduler-1722850837"
    }
    else if (app_name == "acquiring-dolphinscheduler" && app_env == "bureauapacuat") {
        release_name = "dolphinscheduler"
    }
    else if (app_name == "sigma-aggregator-auth-consumer" && app_env == "enbduat") {
        release_name = "aggregator-auth-consumer"
    }
    else if (app_name == "sigma-aggregator-priceauth-dataconsumer" && app_env == "enbduat") {
        release_name = "aggregator-priceauth-dataconsumer"
    }
    else if (app_name == "sigma-payout-axis-connector" && app_env == "enbduat") {
        release_name = "payout-axis-connector"
    }
    else if (app_name == "sigma-payout-processing-service" && app_env == "enbduat") {
        release_name = "payout-processing-service"
    }
    else if (app_name == "sigma-pricing-charge-mgmt-service" && app_env == "enbduat") {
        release_name = "pricing-charge-mgmt-service"
    }
    else if (app_name == "acquiring-apache-pinot" && app_env == "enbduat") {
        release_name = "acquiring-apache-pinot"
    }
    else if (app_name == "acquiring-apache-pinot") {
        release_name = "acquiring-apache-pinot"
    }
    
    else if (app_name == "acquiring-apache-pulsar" && app_env == "bureauapacuat") {
        release_name = "acquiring-apache-pulsar"
    }
     else if (app_name == "acquiring-apache-pulsar" && app_env == "bureaumeauat") {
        release_name = "acquiring-apache-pulsar"
    }

    def finalTag = params.custom_image_tag?.trim() ? 
               params.custom_image_tag.trim() : 
               params.image_tag

    def finalChartVersion = (params.chart_version?.trim() && params.chart_version != "default") ? 
                        params.chart_version.trim() : 
                        params.current_chart_version?.trim()

    def sharedRepoApps = [
    "acquiring-api-apisimulation",
    "acquiring-api-txnmgm",
    "acquiring-api-merchantstlmnt",
    "acquiring-api-mipmgm"
    ]
    def issuingSharedRepoApps = [
    "issuing-api-auth-management",
    "issuing-api-card-management",
    "issuing-api-txn-clients-app",
    "issuing-api-txn-management"
    ]
    echo "selectedApp=${app_name} Hi"
    echo "contains=${sharedRepoApps.contains(app_name?.toString()?.trim())}"

    def chart_name = app_name 
    if (sharedRepoApps.contains(app_name?.toString()) && finalTag) {
        // finalTag = "${params.env}-${app_name}-${finalTag}"
        // finalTag = "${params.env}-acquiring-api-mip-management-${finalTag}"
        chart_name = "acquiring-api"
        values_file_path = "helm_chart/${app_type}/${app_env}/acquiring-api/${app_name}.yaml"
    }
    if (issuingSharedRepoApps.contains(app_name?.toString()?.trim()) && finalTag) {
    // finalTag = "${params.env}-issuing-api-auth-management-${finalTag}"
    chart_name = "issuing-api"
    values_file_path = "helm_chart/${app_type}/${app_env}/issuing-api/${app_name}.yaml"
    }

    echo "application name : ${app_name}"

    echo "Using image tag: ${finalTag}"

    def fullnameOverride = release_name
    if (app_name == "acquiring-helpdesk-dashboard" && app_env == "bureauapacuat") {
        fullnameOverride = "cplus-helpdesk-dashboard"
    }

        else if (app_name == "issuing-apigw-etcd" && app_env == "uat") {
        fullnameOverride = "issuing-apigw-etcd"
    }

     else if (app_name == "issuing-apigw-etcd" && app_env == "uatmeadr") {
        fullnameOverride = "issuing-apigw-etcd"
    }
    
    else if (app_name == "acquiring-apigw-etcd" && app_env == "bureauapacuat") {
        fullnameOverride = "acquiring-apisix-etcd"
    }

     else if (app_name == "acquiring-apigw-etcd" && app_env == "enbduat") {
        fullnameOverride = "acquiring-apisix-etcd"
    }

    else if (app_name == "acquiring-apigw-etcd" && app_env == "bureaumeauat") {
        fullnameOverride = "acquiring-apisix-etcd"
    }
    
    dir("${WORKSPACE}/${repo_dir}") {
        withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {
            logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
            sh "argocd login ${argocd_server_url} --username admin --password $PASSWORD --insecure --grpc-web"

            logger.logger('msg':'Creating or updating ArgoCD application...', 'level':'INFO')
            def applicationYaml

            if (finalTag) {

                if (double_source.toBoolean()) {

                     if (step_params.chart_source == 's3') {
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
    - chart: ${chart_name}
      repoURL: s3://cplus-helm-charts/${app_type}
      targetRevision: ${finalChartVersion}
      helm:
        releaseName: ${release_name}
        valueFiles:
          - \$values/${values_file_path}
        parameters:
          - name: image.tag
            value: ${finalTag}
          - name: fullnameOverride
            value: ${fullnameOverride}
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
    - chart: ${chart_name}
      repoURL: s3://cplus-helm-charts/${app_type}
      targetRevision: ${finalChartVersion}
      helm:
        releaseName: ${release_name}
        valueFiles:
          - \$values/${values_file_path}
        parameters:
          - name: image.tag
            value: ${finalTag}
          - name: fullnameOverride
            value: ${fullnameOverride}
    - repoURL: ${values_repo_url}
      targetRevision: ${values_repo_revision}
      ref: values
                    """
                     }
                     }

                     else {

                                                        
                applicationYaml = """
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${app_name}-${app_env}
  namespace: argocd
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
      path: ${app_name}
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

                        if (step_params.chart_source == 's3') {

                        sh "cat application-${app_name}-${app_env}.yaml && argocd app create -f application-${app_name}-${app_env}.yaml --upsert --grpc-web --validate=false "
                        }
                        else {

                        sh "cat application-${app_name}-${app_env}.yaml && argocd app create -f application-${app_name}-${app_env}.yaml --upsert --grpc-web "
                        }

                        sleep(20)

                        try {

                        if (dry_run.toBoolean() ) {

                            logger.logger('msg':'Skipping Syncing of application as Dry run is enabled', 'level':'WARN')
                            sh "argocd app diff ${app_name}-${app_env} --grpc-web"
                        }
                        } catch (Exception e) {

                                   echo "Changes detected. Please review and decide. "
                                   return
                                }
                         if (!dry_run.toBoolean() )  {

                            if (prune_post_deployment == "true") {
                                sh "argocd app sync ${app_name}-${app_env} --grpc-web"
                            } else {
                                sh "argocd app sync ${app_name}-${app_env} --grpc-web"
                            }
                        }
                }
                else {
                sh """
                echo "Image Tag - ${finalTag}"
                #!/bin/bash
                argocd app create $app_name \
                    --repo $repo_url \
                    --project $argocd_project \
                    --path $helm_chart_path \
                    --values ${values_file_path} \
                    --dest-server ${eks_api_endpoint} \
                    --revision ${repo_branch} \
                    --upsert \
                    --helm-set image.tag=${finalTag}

                sleep 10
                if [ "$prune_post_deployment" = true ]; then
                    argocd app sync ${app_name}-${app_env}
                else
                    argocd app sync ${app_name}-${app_env}
                fi
            """
                }
            }
            else
            {
                sh """
                #!/bin/bash
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
                    #!/bin/bash
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
}
