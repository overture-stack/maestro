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

for example having a single docker file to test and package, doesn't work :

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

## Solution

 - run the tests first
    - ex   
