// Common Parameters
def commonParameters = [
    [
        type: 'string',
        name: 'image_tag',
        defaultValue: 'latest',
        description: 'Docker image tag to deploy'
    ],
    [
        type: 'choice',
        name: 'ENVIRONMENT',
        choices: ['dev', 'uat', 'prod'],
        description: 'Select deployment environment'
    ]
]

def cdJobs = [

    'sdk-instacard-frontend': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/sdk-instacard-frontend/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'design-ui-framework': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/design-ui-framework/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'virtualcard-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/virtualcard-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'instacard-user-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/instacard-user-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'instacard-mock-apis': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/instacard-mock-apis/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'auth-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/auth-service/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],

    'apigateway': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/apigateway/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : commonParameters
    ],
]

cdJobs.each { jobName, config ->

    pipelineJob("CD/${jobName}") {

        displayName(jobName)
        description("CD pipeline for ${jobName} | Owner: ${config.owner}")

        logRotator {
            numToKeep(config.logRotatorNum)
        }

        parameters {
            config.parameters.each { param ->
                switch (param.type) {

                    case 'choice':
                        choiceParam(param.name, param.choices, param.description)
                        break

                    case 'string':
                        stringParam(param.name, param.defaultValue, param.description)
                        break

                    default:
                        stringParam(param.name, param.defaultValue ?: '', param.description)
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