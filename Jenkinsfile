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
  - name: java
    image: openjdk:11-jdk-slim
    command:
    - cat
    tty: true
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
"""
        }
    }
    stages {
        stage('Test') {
            steps {
                container('java') {
                    sh "./mvnw clean install"
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