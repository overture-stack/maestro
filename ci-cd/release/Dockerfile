# This should be changed to download from a nexus artifact repo instead of building here
FROM openjdk:11-jdk as builder
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw install -Dmaven.test.skip=true

FROM openjdk:11-jre-slim
COPY --from=builder /usr/src/app/maestro-app/target/maestro-app-*.jar /usr/bin/maestro-app.jar
CMD ["java", "-jar", "/usr/bin/maestro-app.jar"]
EXPOSE 11235

