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

String podSpec = '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: jdk
    tty: true
    image: openjdk:11-jdk
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
  - name: docker
    image: docker:18-git
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    - name: HOME
      value: /home/jenkins/agent
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
      runAsUser: 0
    volumeMounts:
    - name: docker-graph-storage
      mountPath: /var/lib/docker
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
'''

pipeline {
    agent {
        kubernetes {
            yaml podSpec
        }
    }

    environment {
        dockerHub = 'overture/maestro'
        gitHubRegistry = 'ghcr.io'
        gitHubRepo = 'overture-stack/maestro'
        githubPackages = "${gitHubRegistry}/${gitHubRepo}"

        commit = sh(
            returnStdout: true,
            script: 'git describe --always'
        ).trim()

        version = readMavenPom().getVersion()
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        // run tests and package
        stage('Test') {
            steps {
                container('jdk') {
                    sh './mvnw test package'
                }
            }
        }

        // run tests and package
        stage('build image') {
            steps {
                container('docker') {
                    // the network=host needed to download dependencies using the host network
                    // (since we are inside the 'docker' container)
                    sh "docker build --network=host -f ci-cd/Dockerfile . -t maestro:${commit}"
                }
            }
        }

        stage('Publish images') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                    branch 'master'
                    branch 'test'
                }
            }
            parallel {
                stage('...to dockerhub') {
                    steps {
                        container('docker') {
                            withCredentials([usernamePassword(
                                credentialsId:'OvertureDockerHub',
                                passwordVariable: 'PASSWORD',
                                usernameVariable: 'USERNAME'
                            )]) {
                                sh "docker login -u $USERNAME -p $PASSWORD"

                                script {
                                    if (env.BRANCH_NAME ==~ /(main|master)/) { // push latest and version tags
                                        sh "docker tag maestro:${commit} ${dockerHub}:${version}"
                                        sh "docker push ${dockerHub}:${version}"

                                        sh "docker tag maestro:${commit} ${dockerHub}:latest"
                                        sh "docker push ${dockerHub}:latest"
                                    } else { // push commit tags
                                        sh "docker tag maestro:${commit} ${dockerHub}:${version}-${commit}"
                                        sh "docker push ${dockerHub}:${version}-${commit}"
                                    }

                                    if (env.BRANCH_NAME ==~ /(develop)/) { // push edge tags
                                        sh "docker tag maestro:${commit} ${dockerHub}:edge"
                                        sh "docker push ${dockerHub}:edge"
                                    }
                                }
                            }
                        }
                    }
                }

                stage('...to github') {
                    steps {
                        container('docker') {
                            withCredentials([usernamePassword(
                                credentialsId:'OvertureBioGithub',
                                passwordVariable: 'PASSWORD',
                                usernameVariable: 'USERNAME'
                            )]) {
                                sh "docker login ${gitHubRegistry} -u $USERNAME -p $PASSWORD"

                                script {
                                    if (env.BRANCH_NAME ==~ /(main|master)/) { // push latest and version tags
                                        sh "docker tag maestro:${commit} ${githubPackages}:${version}"
                                        sh "docker push ${githubPackages}:${version}"

                                        sh "docker tag maestro:${commit} ${githubPackages}:latest"
                                        sh "docker push ${githubPackages}:latest"
                                    } else { // push commit tags
                                        sh "docker tag maestro:${commit} ${githubPackages}:${version}-${commit}"
                                        sh "docker push ${githubPackages}:${version}-${commit}"
                                    }

                                    if (env.BRANCH_NAME ==~ /(develop)/) { // push edge tags
                                        sh "docker tag maestro:${commit} ${githubPackages}:edge"
                                        sh "docker push ${githubPackages}:edge"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Release & tag') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(
                        credentialsId: 'OvertureBioGithub',
                        passwordVariable: 'GIT_PASSWORD',
                        usernameVariable: 'GIT_USERNAME'
                    )]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitHubRepo} --tags"
                    }
                }
            }
        }

        stage('Deploy to Overture QA') {
            when {
                branch 'develop'
            }
            steps {
                build(job: '/Overture.bio/provision/helm', parameters: [
                    [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'qa' ],
                    [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'maestro'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'maestro'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: 'https://overture-stack.github.io/charts-server/'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: 'false' ],
                    [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}-${commit}" ]
                ])
            }
        }

        stage('Deploy to Overture Staging') {
            when {
                branch 'master'
            }
            steps {
                build(job: '/Overture.bio/provision/helm', parameters: [
                    [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'staging' ],
                    [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'maestro'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'maestro'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: 'https://overture-stack.github.io/charts-server/'],
                    [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: 'false' ],
                    [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}" ]
                ])
            }
        }
    }

    post {
        fixed {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'fixed_slackChannelURL'
            )]) {
                container('node') {
                    script {
                        if (env.BRANCH_NAME ==~ /(develop|main|master)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"Build Fixed: ${env.JOB_NAME} [Build ${env.BUILD_NUMBER}](${env.BUILD_URL}) \" \
                                }' \
                                ${fixed_slackChannelURL}"
                        }
                    }
                }
            }
        }

        success {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'success_slackChannelURL'
            )]) {
                container('node') {
                    script {
                        if (env.BRANCH_NAME ==~ /(main|master)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"New Maestro published succesfully: v.${version} [Build ${env.BUILD_NUMBER}](${env.BUILD_URL}) \" \
                                }' \
                                ${success_slackChannelURL}"
                        }
                    }
                }
            }
        }

        unsuccessful {
            withCredentials([string(
                credentialsId: 'OvertureSlackJenkinsWebhookURL',
                variable: 'failed_slackChannelURL'
            )]) {
                container('node') {
                    script {
                        if (env.BRANCH_NAME ==~ /(develop|main|master)/) {
                            sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                --data '{ \
                                    \"text\":\"Build Failed: ${env.JOB_NAME} [Build ${env.BUILD_NUMBER}](${env.BUILD_URL}) \" \
                                }' \
                                ${failed_slackChannelURL}"
                        }
                    }
                }
            }
        }
    }
}
