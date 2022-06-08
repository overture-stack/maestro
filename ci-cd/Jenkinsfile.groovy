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

def commit = "UNKNOWN"
def version = "UNKNOWN"
def dockerRepo = "ghcr.io/overture-stack/maestro"
pipeline {
    agent {
        kubernetes {
            label 'maestro-executor'
            yaml """
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
"""
        }
    }
    stages {

        // get the commit and version number for current release
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = sh(returnStdout: true, script: "cat pom.xml | grep \"<version>.*</version>\" | head -1 |awk -F'[><]' '{print \$3}'").trim()
                }
            }
        }

        // run tests and package
        stage('Test') {
            steps {
                container('jdk') {
                    sh "./mvnw test package"
                }
            }
        }

        // run tests and package
        stage('build') {
            steps {
                container('docker') {
                    // the network=host needed to download dependencies using the host network (since we are inside 'docker'
                    // container)
                    sh "docker build --network=host -f ci-cd/Dockerfile . -t overture/maestro:${version}-${commit} -t ${dockerRepo}:${version}-${commit}"
                }
            }
        }

        // publish the edge tag
        stage('Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureBioGithub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker tag ${dockerRepo}:${version}-${commit} ${dockerRepo}:edge"
                    sh "docker push ${dockerRepo}:${version}-${commit}"
                    sh "docker push ${dockerRepo}:edge"
                }
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker tag overture/maestro:${version}-${commit} overture/maestro:edge"
                    sh "docker push overture/maestro:${version}-${commit}"
                    sh "docker push overture/maestro:edge"
               }
            }
        }

       stage('Release') {
           when {
               branch "master"
           }
           steps {
               container('docker') {
                   withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                       sh "git tag ${version}"
                       sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/maestro --tags"
                   }
                   withCredentials([usernamePassword(credentialsId:'OvertureBioGithub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                       sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                   }
                   sh "docker tag ${dockerRepo}:${version}-${commit} ${dockerRepo}:${version}"
                   sh "docker tag ${dockerRepo}:${version}-${commit} ${dockerRepo}:latest"
                   sh "docker push ${dockerRepo}:${version}"
                   sh "docker push ${dockerRepo}:latest"
               }
               container('docker') {
                   withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                       sh 'docker login -u $USERNAME -p $PASSWORD'
                   }
                   sh "docker tag overture/maestro:${version}-${commit} overture/maestro:${version}"
                   sh "docker tag overture/maestro:${version}-${commit} overture/maestro:latest"
                   sh "docker push overture/maestro:${version}"
                   sh "docker push overture/maestro:latest"
              }
           }
       }

	   stage('Deploy to Overture QA') {
		   when {
			   branch "develop"
		   }
		   steps {
			   build(job: "/Overture.bio/provision/helm", parameters: [
					   [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'qa' ],
					   [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'maestro'],
					   [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'maestro'],
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: "https://overture-stack.github.io/charts-server/"],
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: "false" ],
					   [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}-${commit}" ]
			   ])
		   }
	   }

	   stage('Deploy to Overture Staging') {
		   when {
			   branch "master"
		   }
		   steps {
			   build(job: "/Overture.bio/provision/helm", parameters: [
					   [$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'staging' ],
					   [$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'maestro'],
					   [$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'maestro'],
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: ''], // use latest
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: "https://overture-stack.github.io/charts-server/"],
					   [$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: "false" ],
					   [$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}" ]
			   ])
		   }
	   }
    }
}
