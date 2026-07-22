package opstree.nodejs

import opstree.common.*

def unit_testing_factory(Map step_params) {
    logger = new logger()
    if (step_params.unit_testing_check == 'true' || step_params.unit_testing_check == true) {
        unit_test(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Unit Testing. Please mention correct values.', 'level':'WARN')
  }
}

def unit_test(Map step_params) {
    logger = new logger()
    parser = new parser()
    reports_manager = new reports_management()

    logger.logger('msg':'Performing Unit Tests for Node.js', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    fail_job_if_unit_issue_detected = "${step_params.fail_job_if_unit_issue_detected}"
    source_code_path = "${step_params.source_code_path}"
    node_version = step_params.node_version ?: "18"
    build_secret_creds_id = step_params.build_secret_creds_id ?: ''
    build_secret_env_var  = step_params.build_secret_env_var  ?: 'BUILD_SECRET'

    unit_test_reports_path = "${step_params.unit_test_reports_path ?: ''}"
    findbugs_test_report_path = "${step_params.findbugs_test_report_path ?: ''}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    def project_path = "${WORKSPACE}/${repo_dir}${source_code_path ?: ''}"

    dir(project_path) {
        try {
            if (build_secret_creds_id) {
                // Private Azure DevOps npm feed — fetch short-lived token and write .npmrc
                withCredentials([string(credentialsId: build_secret_creds_id, variable: 'THE_SECRET')]) {
                    try {
                        sh """
                            set -e
                            FEED=pkgs.dev.azure.com/msface/SDK/_packaging/AzureAIVision/npm
                            RESPONSE=\$(curl --silent --fail --location \
                                'https://montrafacedev.cognitiveservices.azure.com/face/v1.3-preview.1/settings/getClientAssetsAccessToken' \
                                --header "Ocp-Apim-Subscription-Key: \$THE_SECRET")
                            B64_TOKEN=\$(echo "\$RESPONSE" | jq -r '.base64AccessToken')
                            if [ -z "\$B64_TOKEN" ] || [ "\$B64_TOKEN" = "null" ]; then echo "Token fetch failed"; exit 1; fi
                            {
                              echo "legacy-peer-deps=true"
                              echo "@azure-ai-vision-face:registry=https://\${FEED}/registry/"
                              echo "@azure:registry=https://\${FEED}/registry/"
                              echo "always-auth=true"
                              echo "//\${FEED}/registry/:username=msface"
                              echo "//\${FEED}/registry/:_password=\${B64_TOKEN}"
                              echo "//\${FEED}/registry/:email=not-used@example.com"
                              echo "//\${FEED}/:username=msface"
                              echo "//\${FEED}/:_password=\${B64_TOKEN}"
                              echo "//\${FEED}/:email=not-used@example.com"
                            } > ${project_path}/.npmrc.unittest
                            echo "[INFO] .npmrc.unittest written with Azure feed credentials"
                        """
                        sh """docker run --rm \
                            -v ${project_path}:/app \
                            -v ${project_path}/.npmrc.unittest:/app/.npmrc:ro \
                            -w /app node:${node_version} sh -c "npm install && npm test --if-present" """
                    } finally {
                        sh "rm -f ${project_path}/.npmrc.unittest"
                        logger.logger('msg':'Cleaned up .npmrc.unittest', 'level':'INFO')
                    }
                }
            } else {
                // No private feed credentials — plain npm install
                sh """ docker run --rm -v \${WORKSPACE}/${repo_dir}${source_code_path ?: ''}:/app -w /app node:${node_version} sh -c "npm install && npm test --if-present" """
            }
            if (unit_test_reports_path || findbugs_test_report_path) {
                reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
            }
        }
        catch (Exception e) {
            if (fail_job_if_unit_issue_detected == 'false' || fail_job_if_unit_issue_detected == false) {
                logger.logger('msg':'Unit Test found Issues!! Ignoring as per User inputs', 'level':'WARN')
            }
            else {
                logger.logger('msg':"Unit Test found Issues!!! Unit Testing Failed Error Details: ${e}", 'level':'ERROR')
                error()
            }
        }
    }
}
