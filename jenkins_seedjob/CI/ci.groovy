// Common Parameters
def commonParameters = [
    [
        type: 'choice',
        name: 'BRANCH',
        choices: ['dev', 'uat', 'main'],
        description: 'Select branch to build'
    ],
    [
        type: 'choice',
        name: 'ENVIRONMENT',
        choices: ['dev', 'uat', 'prod'],
        description: 'Select deployment environment'
    ],
    [
        type: 'boolean',
        name: 'TRIGGER_CD',
        defaultValue: true,
        description: 'Trigger CD pipeline after successful CI'
    ]
]

// CI Jobs
def ciJobs = [
    'auth-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/auth-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'instacard-mock-apis': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-mock-apis/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'instacard-user-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-user-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'virtualcard-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/virtualcard-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'sdk-instacard-frontend': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/sdk-instacard-frontend/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'design-ui-framework': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/design-ui-framework/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'apigateway': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/apigateway/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'montra-bom': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/montra-bom/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 5,
        parameters   : commonParameters
    ]
]

// Generate Pipeline Jobs
ciJobs.each { jobName, config ->

    pipelineJob("CI/${jobName}") {

        displayName(jobName)

        description("CI pipeline for ${jobName} | Owner: ${config.owner}")

        logRotator {
            numToKeep(config.logRotatorNum)
        }

        parameters {
            config.parameters.each { param ->

                switch (param.type) {

                    case 'choice':
                        choiceParam(param.name, param.choices, param.description)
                        break

                    case 'boolean':
                        booleanParam(param.name, param.defaultValue, param.description)
                        break

                    default:
                        stringParam(param.name, param.defaultValue, param.description)
                }
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