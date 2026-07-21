def ciJobs = [
    'auth-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/auth-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'instacard-mock-apis': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-mock-apis/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'instacard-user-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-user-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'virtualcard-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/virtualcard-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'sdk-instacard-frontend': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/sdk-instacard-frontend/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
       ]
    ],
    'design-ui-framework': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/design-ui-framework/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'apigateway': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/apigateway/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ],
    'montra-bom': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/montra-bom/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment'],
            [type: 'boolean', name: 'TRIGGER_CD', defaultValue: true, description: 'Trigger CD pipeline after successful CI (Yes/No)'],
            
        ]
    ]
]

ciJobs.each { jobName, config ->
    pipelineJob("CI/${jobName}") {
        displayName("${jobName}")
        description("CI pipeline for ${jobName} | Owner: ${config.owner}")
        logRotator {
            numToKeep(config.logRotatorNum)
        }
        parameters {
            config.parameters.each { param ->
                if (param.type == 'boolean') {
                    booleanParam(param.name, param.defaultValue, param.description)
                } else {
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