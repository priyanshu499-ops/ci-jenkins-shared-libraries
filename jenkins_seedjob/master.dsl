folder('CI') {
    displayName('CI')
    description('Continuous Integration pipelines')
}

folder('CD') {
    displayName('CD')
    description('Continuous Delivery pipelines')
}

folder('user-onboarding') {
    displayName('User Onboarding')
    description('User onboarding and offboarding pipelines — create/delete users, assign/unassign roles')
}

pipelineJob('user-onboarding/user-onboarding') {
    displayName('user-onboarding')
    description('User onboarding pipeline — create Jenkins users and assign roles | Owner: CI-CD Team')
    logRotator {
        numToKeep(10)
    }
    parameters {
        choiceParam('MODE', ['bulk', 'single'], 'Onboarding mode — bulk (CSV) or single user')
        stringParam('USERNAME', '', 'Username (only for single mode)')
        stringParam('EMAIL', '', 'Email address (only for single mode)')
        stringParam('ROLES', 'apigateway-read,apigateway-execute,auth-service-read,auth-service-execute,design-ui-framework-read,design-ui-framework-execute,instacard-mock-apis-read,instacard-mock-apis-execute,instacard-user-service-read,instacard-user-service-execute,montra-bom-read,montra-bom-execute,sdk-instacard-frontend-read,sdk-instacard-frontend-execute,virtualcard-service-read,virtualcard-service-execute', 'Comma-separated roles to assign (only for single mode)')
        stringParam('CSV_PATH', 'resources/user-onboarding/users.csv', 'Path to users CSV file (for bulk mode)')
        booleanParam('SEND_EMAIL', true, 'Send credentials email to user after creation')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git')
                        credentials('github-token')
                    }
                    branch('main')
                }
            }
            scriptPath('jenkins_wrapper/user-onboarding/Jenkinsfile')
            lightweight(true)
        }
    }
}

pipelineJob('user-onboarding/user-offboarding') {
    displayName('user-offboarding')
    description('User offboarding pipeline — delete Jenkins users and unassign roles | Owner: CI-CD Team')
    logRotator {
        numToKeep(10)
    }
    parameters {
        choiceParam('MODE', ['single', 'bulk'], 'Offboarding mode — single user or bulk (CSV)')
        stringParam('USERNAME', '', 'Username to delete (only for single mode)')
        stringParam('CSV_PATH', 'resources/user-onboarding/users.csv', 'Path to users CSV file (for bulk mode)')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git')
                        credentials('github-token')
                    }
                    branch('main')
                }
            }
            scriptPath('jenkins_wrapper/user-offboarding/Jenkinsfile')
            lightweight(true)
        }
    }
}
