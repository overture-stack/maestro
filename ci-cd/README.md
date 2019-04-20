# CI/CD Rational

## Goal:
- have a kuberentes based Jenkins CI/CD setup to build, dockerize, push maestro artifacts

## Context:
- we use kubernetes jenkins plugin to spin up agents based on pod yaml specs:

```groovy
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
        // stages here
    }
}


```
this pod has a container with docker client setup there to be able to build/push docker images.


## Challange
- using testcontainers requires access to a docker daemon to manage containers


# Attempts

## Attempt 1
for example having a single docker file to test and package, and the following jenkins script fails :

```Dockerfile
FROM openjdk:11-jdk as builder
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw test install 

FROM openjdk:11-jre-slim
COPY --from=builder /usr/src/app/maestro-app/target/maestro-app-*.jar /usr/bin/maestro-app.jar
CMD ["java", "-jar", "/usr/bin/maestro-app.jar"]
EXPOSE 11235
```

```groovy
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
                    // the netowrk=host needed to download dependencies using the host network (since we are inside 'docker'
                    // container)
                    sh "docker  build --network=host . -t overture/maestro:${commit}"
                }
            }
        }
    }
}
```

Error :

```
2019-04-18 | 22:04:32.805 | 97 | ducttape-0 | DEBUG | com.github.dockerjava.core.command.AbstrDockerCmd | Cmd: org.testcontainers.dockerclient.transport.okhttp.OkHttpDockerCmdExecFactory$1@cf8ad01

2019-04-18 | 22:04:33.065 | 97 | main | ERROR | org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy | ping failed with configuration Environment variables, system properties and defaults. Resolved: 
    dockerHost=unix:///var/run/docker.sock
    apiVersion='{UNKNOWN_VERSION}'
    registryUrl='https://index.docker.io/v1/'
    registryUsername='root'
    registryPassword='null'
    registryEmail='null'
    dockerConfig='DefaultDockerClientConfig[dockerHost=unix:///var/run/docker.sock,registryUsername=root,registryPassword=<null>,registryEmail=<null>,registryUrl=https://index.docker.io/v1/,dockerConfigPath=/root/.docker,sslConfig=<null>,apiVersion={UNKNOWN_VERSION},dockerConfig=<null>]'

 due to org.rnorth.ducttape.TimeoutException: Timeout waiting for result with exception
org.rnorth.ducttape.TimeoutException: Timeout waiting for result with exception
	at org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess(Unreliables.java:51)
	at org.testcontainers.dockerclient.DockerClientProviderStrategy.ping(DockerClientProviderS
```

Although the socket is mounted on the 'docker' container, it's not visible for test containers in the docker build process
because (a theory) build runs another container where the socket is not mounted and cannot be mounted.

proof, this fails in plain docker locally :

```
    Dockerfile:
    FROM docker
    RUN docker run -v /var/run/docker.sock:/var/run/docker.sock alpine echo hi
    
    $ docker build . -t xyz 
    
    docker: Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?.
    
```

https://stackoverflow.com/questions/51857634/cannot-connect-to-the-docker-daemon-at-unix-var-run-docker-sock-is-the-docke



# Attempt 2

Try to have a tests run in a jdk contianer outside Dockerfile before building the image 
```groovy
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
  - name: tester
    image: openjdk:11-jdk
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock    
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
                container('tester') {
                    sh './mvnw test'
                }
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    script {
                        commit = sh(returnStdout: true, script: 'git describe --always').trim()
                    }
                    // the netowrk=host needed to download dependencies using the host network (since we are inside 'docker'
                    // container)
                    sh "docker  build --network=host . -t overture/maestro:${commit}"
                }
            }
        }
    }
}
```

This does allow testcontainers to create containers. However, the problem is that those containers are on a different 
network than this pod and this tester container cannot reach them.

I tried defining the pod with `hostNetwork: true` but it never started (seems like jenkins k8s plugin issue).


## Solution
To solve the socket availability issue at build time we can use `docker run -v /var/run/docker.sock:.. etc`
so something like this may seem promising:

```groovy
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
                    sh "docker run --rm -v `pwd`:`pwd` -w `pwd` -v /var/run/docker.sock:/var/run/docker.sock openjdk:11-jdk ./mvnw test"
                    sh "docker build --network=host -f Dockerfile.release . -t overture/maestro:${commit}"
                }
            }
        }
    }

```

but it fails short in Jenkins kubernetes plugin because:

- the workspace is a mounted directory in the container : docker
- you can't mount that mounted directory again 

proof:

```bash




```