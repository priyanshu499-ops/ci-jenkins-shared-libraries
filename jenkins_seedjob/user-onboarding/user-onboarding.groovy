def onboardingJobs = [
    'users': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/user-onboarding/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum: 10,
        parameters   : [
            [type: 'choice', name: 'MODE', choices: 'bulk\nsingle', description: 'Onboarding mode — bulk (CSV) or single user'],
            [name: 'USERNAME', defaultValue: '', description: 'Username (only for single mode)'],
            [name: 'EMAIL', defaultValue: '', description: 'Email address (only for single mode)'],
            [name: 'ROLES', defaultValue: 'issuing-read,issuing-execute,acquiring-read,acquiring-execute,sigma-read,sigma-execute', description: 'Comma-separated roles to assign (only for single mode). Available roles: issuing-read, issuing-execute, acquiring-read, acquiring-execute, sigma-read, sigma-execute'],
            [name: 'CSV_PATH', defaultValue: 'resources/user-onboarding/users.csv', description: 'Path to users CSV file (for bulk mode)'],
            [type: 'boolean', name: 'SEND_EMAIL', defaultValue: false, description: 'Send credentials email to user after creation']
        ]
    ]
]

onboardingJobs.each { jobName, config ->
    pipelineJob("user-onboarding/${jobName}") {
        displayName("users")
        description("User onboarding pipeline — create Jenkins users and assign roles | Owner: ${config.owner}")
        logRotator {
            numToKeep(config.logRotatorNum)
        }
        parameters {
            config.parameters.each { param ->
                if (param.type == 'boolean') {
                    booleanParam(param.name, param.defaultValue, param.description)
                } else if (param.type == 'choice') {
                    choiceParam(param.name, param.choices.split('\n') as List, param.description)
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
