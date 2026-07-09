def cdJobs = [
    'spring-boot-realworld': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/spring-boot-realworld/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'simple-nodejs-app': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/simple-nodejs-app/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'sdk-instacard-frontend': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/sdk-instacard-frontend/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'design-ui-framework': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/design-ui-framework/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'virtualcard-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/virtualcard-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'instacard-user-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/instacard-user-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'instacard-mock-apis': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/instacard-mock-apis/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'auth-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/auth-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
     'apigateway': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/apigateway/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
]

cdJobs.each { jobName, config ->
    pipelineJob("CD/${jobName}") {
        displayName("${jobName}")
        description("CD pipeline for ${jobName} | Owner: ${config.owner}")
        logRotator {
            numToKeep(config.logRotatorNum)
        }
        parameters {
            config.parameters.each { param ->
                stringParam(param.name, param.defaultValue, param.description)
            }
        }
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(config.url)
                            credentials(config.credentials)
                        }
                        branch(config.branch)
                    }
                }
                scriptPath(config.scriptPath)
                lightweight(true)
            }
        }
    }
}