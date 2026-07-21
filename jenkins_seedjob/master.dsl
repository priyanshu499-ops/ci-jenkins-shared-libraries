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

try {
    evaluate(readFileFromWorkspace('jenkins_seedjob/USER_MANAGEMENT/user_management.groovy'))
} catch (Exception e) {
    println("Note: evaluating user_management.groovy from master.dsl: " + e.message)
}
