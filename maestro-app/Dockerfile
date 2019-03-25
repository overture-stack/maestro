FROM adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine

# unpacks spring boot uber jar
# To take advantage of the clean separation between dependencies and application resources in a Spring Boot
# fat jar file, we will use a slightly different implementation of the Dockerfile
# see: https://spring.io/guides/gs/spring-boot-docker/
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
VOLUME /app/config
EXPOSE 11235

ENTRYPOINT ["java","-cp","app:app/lib/*","bio.overture.maestro.app.Maestro"]