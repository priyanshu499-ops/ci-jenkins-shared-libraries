package opstree.common

import opstree.common.*

def image_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.image_scanning_check == 'true') {
        trivy(step_params)
    } else {
        logger.logger('msg':'No valid option selected for image scanning. Please mention correct values.', 'level':'WARN')
    }
}

def trivy(Map step_params) {
    def logger              = new logger()
    def parser              = new parser()
    def image_scanning_reports = new reports_management()

    logger.logger('msg':'Performing Image Scanning', 'level':'INFO')

    def image_scanning_report_publish = "${step_params.image_scanning_report_publish}"
    def fail_job_if_scan_failed       = "${step_params.fail_job_if_scan_failed ?: 'false'}"
    def trivyCacheDir                 = "${JENKINS_HOME}/trivy-cache"
    def javaDbPath                    = "${trivyCacheDir}/fanal/javadb"

    dir("${WORKSPACE}") {
        sh "mkdir -p ${WORKSPACE}/trivy"
        sh "mkdir -p ${trivyCacheDir}"
        sh "sudo chmod -R 777 ${WORKSPACE}/trivy ${trivyCacheDir}"

        // ── Java DB existence check ──────────────────────────────────────────
        // trivy-java-db is ~892 MiB and takes 2+ min to download.
        // Once downloaded, reuse it indefinitely — only re-download if the DB
        // is completely absent (first run or manually cleared).
        // The 24-hour staleness window has been intentionally removed to avoid
        // intermittent OCI download failures on ghcr.io during every stale build.
        def skipJavaDbFlag = sh(script: """
            JAVA_DB_META="${trivyCacheDir}/fanal/javadb/metadata.json"
            if [ -f "\$JAVA_DB_META" ]; then
                echo "--skip-java-db-update"
            else
                echo ""
            fi
        """, returnStdout: true).trim()

        if (skipJavaDbFlag == '--skip-java-db-update') {
            logger.logger('msg':'Trivy Java DB already exists on disk - skipping Java DB download (reusing cached copy)', 'level':'INFO')
        } else {
            logger.logger('msg':'Trivy Java DB not found - downloading Java DB for the first time', 'level':'INFO')
        }

        try {
            def imageExists = sh(
                script: "docker images -q ${step_params.image_name}:${step_params.image_tag}",
                returnStdout: true
            ).trim()

            if (imageExists) {
                logger.logger('msg':'Image found, proceeding with Trivy scan', 'level':'INFO')

                // ── Write the render resources into trivy dir ───────────────────
                // We use libraryResource because shared library resources are not
                // automatically present in the agent workspace checkout.
                dir("${WORKSPACE}/trivy") {
                    writeFile file: 'report.html',
                              text: libraryResource('trivy/render/report.html')
                    writeFile file: 'report.css',
                              text: libraryResource('trivy/render/report.css')
                    writeFile file: 'inject.sh',
                              text: libraryResource('trivy/render/inject.sh')
                    sh 'chmod +x inject.sh'
                }


                // --scanners vuln   → disable secret scanning (saves ~30s and avoids
                //                     the "scanning is slow" warning in Trivy logs)
                // --skip-java-db-update → use cached Java DB when it is still fresh
                // 1. Run Trivy and output JSON
                // 2. Run Python script to convert JSON to beautiful HTML
                sh """
                    docker run --rm \\
                        -v /var/run/docker.sock:/var/run/docker.sock \\
                        -v ${WORKSPACE}/trivy:/output \\
                        -v ${trivyCacheDir}:/root/.cache/trivy \\
                        -e IMAGE_NAME="${step_params.image_name}" \\
                        -e IMAGE_TAG="${step_params.image_tag}" \\
                        -e SCAN_SEVERITY="${step_params.scan_severity}" \\
                        aquasec/trivy:0.56.0 image \\
                            --scanners vuln \\
                            --severity "${step_params.scan_severity}" \\
                            ${skipJavaDbFlag} \\
                            --format json \\
                            --output /output/trivy_report.json \\
                            ${step_params.image_name}:${step_params.image_tag}
                            
                    cd ${WORKSPACE}/trivy
                    ./inject.sh
                """
                logger.logger('msg':'Trivy scan completed successfully', 'level':'INFO')
            } else {
                logger.logger('msg':"Image ${step_params.image_name}:${step_params.image_tag} not found locally. Build may not have run.", 'level':'ERROR')
            }

            if (image_scanning_report_publish == 'true') {
                logger.logger('msg':'Publishing Trivy Image Scanning Report', 'level':'INFO')
                image_scanning_reports.publish(
                    'report_dir' : "${WORKSPACE}/trivy",
                    'report_file': 'trivy_report.html',
                    'report_name': 'Trivy Image Scanning Report'
                )
            } else {
                logger.logger('msg':'Trivy Image Scanning Report Publishing Skipped', 'level':'INFO')
            }

        } catch (Exception e) {
            logger.logger('msg':"Trivy scan failed: ${e.message}", 'level':'ERROR')
            if (fail_job_if_scan_failed == 'true') {
                error "Trivy scan failed: ${e.message}"
            } else {
                logger.logger('msg':'Trivy scan failed but ignoring as per user config.', 'level':'WARN')
            }
        }
    }
}
