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
    
    unit_test_reports_path = "${step_params.unit_test_reports_path ?: ''}"
    findbugs_test_report_path = "${step_params.findbugs_test_report_path ?: ''}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/${repo_dir}${source_code_path ?: ''}") {
        try {
            sh """ docker run --rm -v \${WORKSPACE}/${repo_dir}${source_code_path ?: ''}:/app -w /app node:${node_version} sh -c "npm install && npm test --if-present" """
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
