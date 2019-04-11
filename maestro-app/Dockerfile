FROM adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine

# unpacks spring boot uber jar
# To take advantage of the clean separation between dependencies and application resources in a Spring Boot
# fat jar file, we will use a slightly different implementation of the Dockerfile
# see: https://spring.io/guides/gs/spring-boot-docker/
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

# for development config changes no need to mount in prod.
VOLUME /app/config

# needed for the failure log
VOLUME /logs/maestro

# maestro web api port
EXPOSE 11235

ENTRYPOINT ["java","-cp","app:app/lib/*","bio.overture.maestro.app.Maestro"]