def commit = "UNKNOWN"
def version = "UNKNOWN"
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
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
    volumeMounts:
    - name: docker-graph-storage
      mountPath: /var/lib/docker
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
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
                    version = sh(returnStdout: true, script: "cat ./.mvn/maven.config | grep revision | cut -d '=' -f2").trim()
                }
            }
        }

        // run tests and package
        stage('Test') {
            steps {
                container('jdk') {
                    // remove the snapshot and append the commit (the dot before ${commit} is intentional)
                    // this does NOT publish to a maven artifacts store
                    sh "./mvnw -Dsha1=.${commit} -Dchangelist=-${BUILD_NUMBER} test package"
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
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }

                    // the netowrk=host needed to download dependencies using the host network (since we are inside 'docker'
                    // container)
                    sh "docker  build --network=host -f ci-cd/Dockerfile . -t overture/maestro:edge"
                    sh "docker push overture/maestro:edge"
               }
            }
        }

        // Publish the release candidate artifacts
        // to create a release candidate branch see tasks folder under ci-cd
        stage('Publish Release Candidate') {
            when {
                branch "rc/${version}-${commit}"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker  build --network=host -f ci-cd/Dockerfile . -t overture/maestro:${version}.${commit}-RC"
                    sh "docker push overture/maestro:${version}.${commit}-RC"

               }
            }
        }

       /*
        * Release & Delivery
        */
        stage('Deploy QA') {
            when {
                branch "rc/${version}-${commit}"
            }
            steps {
                container('helm') {
                    withCredentials([file(credentialsId:'4ed1e45c-b552-466b-8f86-729402993e3b', variable: 'QA_KUBECONFIG')]) {
                        sh 'helm init --client-only'
                        sh 'helm ls --kubeconfig $QA_KUBECONFIG'
                        sh 'helm repo add overture  https://overture-stack.github.io/charts-server/'
                        sh "helm upgrade --install maestro-qa overture/maestro -f ci-cd/chart-values/values.qa.yaml --set image.tag=${version}.${commit}"
                    }
                }
            }
        }

        stage('Release') {
            when {
                branch "master"
            }
            steps {
//                container('jdk') {
//                    sh "./mvnw -Dsha1= -Dchangelist= -DskipTests deploy"
//                }
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker  build --network=host -f ci-cd/Dockerfile . -t overture/maestro:latest -t overture/maestro:${version}"
                    sh "docker push overture/maestro:${verison}"
                    sh "docker push overture/maestro:latest"
                    withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/maestro --tags"
                    }
               }
            }
        }

        stage('Deploy Production') {
            when {
                branch "master"
            }
            steps {
                container('helm') {
                    withCredentials([file(credentialsId:'4ed1e45c-b552-466b-8f86-729402993e3b', variable: 'PR_KUBECONFIG')]) {
                        sh 'helm init --client-only'
                        sh 'helm ls --kubeconfig $PR_KUBECONFIG'
                        sh 'helm repo add overture  https://overture-stack.github.io/charts-server/'
                        sh "helm upgrade --install maestro-pr overture/maestro -f ci-cd/chart-values/values.pr.yaml --set image.tag=${version}"
                    }
                }
            }
        }
    }

    post {
        always {
            junit "**/TEST-*.xml"
        }
    }
}