def commit = "UNKNOWN"

pipeline {
    agent {
        kubernetes {
            label 'maestro-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
"""
        }
    }
    stages {
        stage('Build') {
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    script {
                        commit = sh(returnStdout: true, script: 'git describe --always').trim()
                    }
                    sh "docker build -f Dockerfile.test . -t maestro-src:latest"
                    sh "docker --network=host run -v /var/run/docker.sock:/var/run/docker.sock maestro-src:latest /src/mvnw test"
                    // sh "docker build --network=host -f Dockerfile.release . -t overture/maestro:${commit}"
                }
            }
        }
    }
}