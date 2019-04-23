def commit=""
def version=""

pipeline {
    agent {
        kubernetes {
            label 'maestro-executor'
        }
    }
    stages {
        // get the commit and version number for current release
        stage('Cut Release Candidate Branch') {
            when {
                branch "develop"
            }
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = sh(returnStdout: true, script: "cat ./.mvn/maven.config | grep revision | cut -d '=' -f2").trim()
                }
                sh "git checkout -b rc/${version}-${commit}"
                sh "git push --set-upstream origin rc/${version}-${commit}"
            }
        }
    }
}