folder('CI') {
    displayName('CI')
    description('Continuous Integration pipelines')
}

folder('CD') {
    displayName('CD')
    description('Continuous Delivery pipelines')
}

folder('user-management') {
    displayName('User Management')
    description('User onboarding and offboarding pipelines')
}

// Evaluate user_management.groovy script so pipelines are generated even if not listed in Seed Job targets
evaluate(readFileFromWorkspace('jenkins_seedjob/USER_MANAGEMENT/user_management.groovy'))
