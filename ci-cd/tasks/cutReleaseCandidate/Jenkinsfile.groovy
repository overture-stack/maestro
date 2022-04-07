/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
                    version = sh(returnStdout: true, script: "cat pom.xml | grep \"<version>.*</version>\" | head -1 |awk -F'[><]' '{print \$3}'").trim()
                }
                withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    sh "git checkout -b rc/${version}-${commit}"
                    sh "git push --set-upstream https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/maestro rc/${version}-${commit}"
                }
            }
        }
    }
}