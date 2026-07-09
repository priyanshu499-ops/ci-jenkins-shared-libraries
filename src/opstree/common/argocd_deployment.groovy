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

    // ── Resolve app_type / app_env (null-safe) ──
    def raw_app_type           = step_params.app_type?.toString()?.trim()
    def raw_app_env            = step_params.app_env?.toString()?.trim()
    def app_type               = (raw_app_type && raw_app_type != 'null') ? raw_app_type : ""
    def app_env                = (raw_app_env  && raw_app_env  != 'null') ? raw_app_env  : ""
    // argocd_project: use explicit override if provided, else build from app_type+app_env, else just app_env
    def raw_argocd_project     = step_params.argocd_project?.toString()?.trim()
    def argocd_project         = (raw_argocd_project && raw_argocd_project != 'null')
                                    ? raw_argocd_project
                                    : (app_type && app_env) ? "${app_type}-${app_env}" : (app_env ?: "default")
    def argocd_app_name        = app_env ? "${step_params.app_name}-${app_env}" : "${step_params.app_name}"
    // ─────────────────────────────────────────────

    def app_name               = "${step_params.app_name}"
    def argocd_credential_id    = "${step_params.argocd_credential_id}"
    def argocd_server_env_name  = "${step_params.argocd_server_env_name}"
    def argocd_server_url       = env[argocd_server_env_name]
    def eks_api_endpoint_env_name = step_params.eks_api_endpoint_env_name?.toString()?.trim() ?: ""
    def eks_api_endpoint       = (eks_api_endpoint_env_name && env[eks_api_endpoint_env_name]) ? env[eks_api_endpoint_env_name] : "https://kubernetes.default.svc"
    def destination_namespace  = step_params.destination_namespace?.toString()?.trim() ?: app_env ?: "default"
    def prune_post_deployment  = step_params.prune_post_deployment?.toString()?.toBoolean() ?: false
    def perform_health_check   = step_params.perform_health_check?.toString()?.toBoolean() ?: false
    def dry_run                = step_params.dry_run?.toString()?.toBoolean() ?: false

    // ── Source repo (the gitops / helm-chart repo) ──
    def repo_url               = step_params.repo_url?.toString()?.trim() ?: ""
    def repo_branch            = step_params.repo_branch?.toString()?.trim() ?: "main"

    // ── Values repo (same gitops repo or a separate one) ──
    // If values_repo_url is provided use it, otherwise fall back to repo_url
    def values_repo_url        = step_params.values_repo_url?.toString()?.trim() ?: repo_url
    def values_repo_revision   = step_params.values_repo_revision?.toString()?.trim() ?: repo_branch

    // ── Helm chart source path inside the repo (e.g. gitops-repo/helm) ──
    def helm_chart_path        = step_params.helm_chart_path?.toString()?.trim() ?: ""

    // ── Values file path (e.g. applications/dev/auth-service.yaml) ──
    def values_file_path       = step_params.values_file_path?.toString()?.trim() ?: "applications/${app_env}/${app_name}.yaml"

    // ── Image tag: prefer custom_image_tag param, fall back to image_tag param ──
    def finalTag = params?.custom_image_tag?.trim() ? params.custom_image_tag.trim() : params?.image_tag?.trim() ?: ""

    logger.logger('msg':"Target Application  : ${argocd_app_name}", 'level':'INFO')
    logger.logger('msg':"ArgoCD Project      : ${argocd_project}", 'level':'INFO')
    logger.logger('msg':"Destination Server  : ${eks_api_endpoint}", 'level':'INFO')
    logger.logger('msg':"Destination NS      : ${destination_namespace}", 'level':'INFO')
    logger.logger('msg':"Helm chart path     : ${helm_chart_path}", 'level':'INFO')
    logger.logger('msg':"Values file         : ${values_file_path}", 'level':'INFO')
    if (finalTag) {
        logger.logger('msg':"Image tag           : ${finalTag}", 'level':'INFO')
    }

    withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {

        logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
        sh """
        #!/bin/bash
        export XDG_CONFIG_HOME=\$(pwd)/.config
        argocd login ${argocd_server_url} --username admin --password \$PASSWORD --insecure --grpc-web
        """

        // ──────────────────────────────────────────────────────────────────────────────
        // Build the ArgoCD Application YAML  (pinelabs-style "app create -f --upsert")
        // This creates the app if it doesn't exist AND updates it if it already does.
        //
        // Two modes:
        //   helm_chart_path set  → double-source (chart path + $values ref)
        //   helm_chart_path empty → single-source (helm inside same repo)
        // ──────────────────────────────────────────────────────────────────────────────

        def imageParamBlock = finalTag ? """
        parameters:
          - name: image.tag
            value: ${finalTag}""" : ""

        def syncPolicyBlock = dry_run ? "" : """
  syncPolicy:
    automated:
      prune: false
      selfHeal: true
    syncOptions:
      - CreateNamespace=true"""

        def applicationYaml

        if (helm_chart_path) {
            // ── Double-source: helm chart at helm_chart_path + values via $values ref ──
            applicationYaml = """
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${argocd_app_name}
  namespace: argocd
  annotations:
    argocd.argoproj.io/compare-options: ServerSideDiff=true
spec:
  project: ${argocd_project}
${syncPolicyBlock}
  destination:
    server: ${eks_api_endpoint}
    namespace: ${destination_namespace}

  sources:
    - repoURL: ${repo_url}
      targetRevision: ${repo_branch}
      path: ${helm_chart_path}
      helm:
        releaseName: ${app_name}
        valueFiles:
          - \$values/${values_file_path}
        ${imageParamBlock}
    - repoURL: ${values_repo_url}
      targetRevision: ${values_repo_revision}
      ref: values
"""
        } else {
            // ── Single-source: chart and values in the same repo ──
            applicationYaml = """
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${argocd_app_name}
  namespace: argocd
  annotations:
    argocd.argoproj.io/compare-options: ServerSideDiff=true
spec:
  project: ${argocd_project}
${syncPolicyBlock}
  destination:
    server: ${eks_api_endpoint}
    namespace: ${destination_namespace}

  source:
    repoURL: ${repo_url}
    targetRevision: ${repo_branch}
    helm:
      releaseName: ${app_name}
      valueFiles:
        - ${values_file_path}
      ${imageParamBlock}
"""
        }

        // Write manifest to workspace
        writeFile file: "application-${argocd_app_name}.yaml", text: applicationYaml

        if (dry_run) {
            // ── DRY-RUN: upsert the manifest (no automated sync), then show diff ──
            // Same logic as pinelabs: diff exits non-zero if changes exist → catch → return
            logger.logger('msg':'[DRY-RUN] Upserting ArgoCD Application manifest (no sync)...', 'level':'WARN')
            sh """
            #!/bin/bash
            export XDG_CONFIG_HOME=\$(pwd)/.config
            cat application-${argocd_app_name}.yaml
            argocd app create -f application-${argocd_app_name}.yaml --upsert --grpc-web --validate=false
            """
            sleep(20)
            try {
                logger.logger('msg':'[DRY-RUN] Showing diff of pending changes...', 'level':'WARN')
                sh """
                #!/bin/bash
                export XDG_CONFIG_HOME=\$(pwd)/.config
                argocd app diff ${argocd_app_name} --grpc-web
                """
            } catch (Exception e) {
                echo "Changes detected. Please review and decide."
                return
            }

        } else {
            // ── ACTUAL DEPLOY: upsert manifest then sync ──
            logger.logger('msg':'Creating or Updating ArgoCD Application via manifest...', 'level':'INFO')
            sh """
            #!/bin/bash
            export XDG_CONFIG_HOME=\$(pwd)/.config
            cat application-${argocd_app_name}.yaml
            argocd app create -f application-${argocd_app_name}.yaml --upsert --grpc-web --validate=false
            """
            sleep(20)

            if (prune_post_deployment) {
                logger.logger('msg':'Syncing application with prune enabled...', 'level':'INFO')
                sh """
                #!/bin/bash
                export XDG_CONFIG_HOME=\$(pwd)/.config
                argocd app sync ${argocd_app_name} --prune --grpc-web
                """
            } else {
                logger.logger('msg':'Syncing application...', 'level':'INFO')
                sh """
                #!/bin/bash
                export XDG_CONFIG_HOME=\$(pwd)/.config
                argocd app sync ${argocd_app_name} --grpc-web
                """
            }
        }

        // ── Health check (skipped in dry-run) ──
        if (!dry_run && perform_health_check) {
            logger.logger('msg':'Checking Application Health. Please wait...', 'level':'INFO')
            sh """
                #!/bin/bash
                export XDG_CONFIG_HOME=\$(pwd)/.config
                MAX_RETRIES=10
                RETRY_INTERVAL=30
                RETRIES=0

                while [ \$RETRIES -lt \$MAX_RETRIES ]; do
                    APP_STATUS=\$(argocd app get ${argocd_app_name} --output json --grpc-web | jq -r '.status.health.status')
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
        } else if (dry_run) {
            logger.logger('msg':'Dry run completed. Skipping health check.', 'level':'INFO')
        } else {
            logger.logger('msg':'Health check skipped (perform_health_check=false).', 'level':'INFO')
        }
    }
}