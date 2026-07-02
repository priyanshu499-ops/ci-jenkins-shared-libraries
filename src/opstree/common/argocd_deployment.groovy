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

    logger.logger('msg':'Starting ArgoCD Deployment Step', 'level':'INFO')

    def app_name             = "${step_params.app_name}"
    def repo_url             = "${step_params.repo_url}"
    def argocd_credential_id = "${step_params.argocd_credential_id}"
    def argocd_server_env_name = "${step_params.argocd_server_env_name}"
    def argocd_server_url    = env[argocd_server_env_name]
    def prune_post_deployment = "${step_params.prune_post_deployment}"
    def helm_chart_path      = "${step_params.helm_chart_path}"
    def values_file_path     = "${step_params.values_file_path}"
    def repo_branch          = "${step_params.repo_branch}"
    def source_code_path     = step_params.source_code_path ?: ''
    def dry_run              = step_params.dry_run?.toString()?.toBoolean() ?: false

    // Default to in-cluster if eks_api_endpoint_env_name is not provided
    def eksEnvName     = step_params.eks_api_endpoint_env_name
    def eks_api_endpoint = (eksEnvName && env[eksEnvName]) ? env[eksEnvName] : 'https://kubernetes.default.svc'

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}") + source_code_path

    dir("${WORKSPACE}/${repo_dir}") {
        withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {
            logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
            sh """
            #!/bin/bash
            export XDG_CONFIG_HOME=\$(pwd)/.config
            argocd login ${argocd_server_url} --username admin --password \$PASSWORD --insecure
            """

            if (dry_run) {
                // ── DRY-RUN: Create/update app definition (no auto-sync) and show diff ──
                logger.logger('msg':'[DRY-RUN] Updating ArgoCD app definition and showing diff...', 'level':'INFO')
                if (params.image_tag) {
                    sh """
                    #!/bin/bash
                    export XDG_CONFIG_HOME=\$(pwd)/.config
                    argocd app create ${app_name} \\
                        --repo ${repo_url} \\
                        --path ${helm_chart_path} \\
                        --values ${values_file_path} \\
                        --dest-server ${eks_api_endpoint} \\
                        --revision ${repo_branch} \\
                        --upsert \\
                        --helm-set image.tag=${params.image_tag}

                    sleep 5
                    echo "=== DRY-RUN: Changes that will be applied ==="
                    argocd app get ${app_name} --refresh > /dev/null || true
                    PAGER="" argocd app diff ${app_name} --exit-code=false
                    echo "=== DRY-RUN Complete ==="
                    """
                } else {
                    sh """
                    #!/bin/bash
                    export XDG_CONFIG_HOME=\$(pwd)/.config
                    argocd app create ${app_name} \\
                        --repo ${repo_url} \\
                        --path ${helm_chart_path} \\
                        --values ${values_file_path} \\
                        --dest-server ${eks_api_endpoint} \\
                        --revision ${repo_branch} \\
                        --upsert

                    sleep 5
                    echo "=== DRY-RUN: Changes that will be applied ==="
                    argocd app get ${app_name} --refresh > /dev/null || true
                    PAGER="" argocd app diff ${app_name} --exit-code=false
                    echo "=== DRY-RUN Complete ==="
                    """
                }
                logger.logger('msg':'[DRY-RUN] Diff complete. Awaiting manual approval before actual deployment.', 'level':'INFO')

            } else {
                // ── ACTUAL DEPLOY: Create/update app definition. User syncs manually from ArgoCD ──
                logger.logger('msg':'Updating ArgoCD application definition (no auto-sync)...', 'level':'INFO')
                if (params.image_tag) {
                    sh """
                    #!/bin/bash
                    export XDG_CONFIG_HOME=\$(pwd)/.config
                    argocd app create ${app_name} \\
                        --repo ${repo_url} \\
                        --path ${helm_chart_path} \\
                        --values ${values_file_path} \\
                        --dest-server ${eks_api_endpoint} \\
                        --revision ${repo_branch} \\
                        --upsert \\
                        --helm-set image.tag=${params.image_tag}
                    """
                } else {
                    sh """
                    #!/bin/bash
                    export XDG_CONFIG_HOME=\$(pwd)/.config
                    argocd app create ${app_name} \\
                        --repo ${repo_url} \\
                        --path ${helm_chart_path} \\
                        --values ${values_file_path} \\
                        --dest-server ${eks_api_endpoint} \\
                        --revision ${repo_branch} \\
                        --upsert
                    """
                }
                logger.logger('msg':'ArgoCD app definition updated. Please sync manually from ArgoCD UI.', 'level':'INFO')
            }
        }
    }
}
