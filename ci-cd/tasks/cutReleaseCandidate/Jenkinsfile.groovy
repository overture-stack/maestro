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
            steps {
                sh "git --version"
                sh "git clone https://github.com/overture-stack/maestro ."
                sh "git checkout develop"
                script{
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = sh(returnStdout: true, script: "cat ./.mvn/maven.config | grep revision | cut -d '=' -f2").trim()
                }
                withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    sh "git checkout -b rc/${version}-${commit}"
                    sh "git push --set-upstream https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/maestro rc/${version}-${commit}"
                }
            }
        }
    }
}