FROM openjdk:11-jdk as builder
WORKDIR /usr/src/app
COPY . .
# tests to be executed outside of dockerfile because we use testcontainers
# needs access to docker socket
RUN ./mvnw clean package -DskipTests

FROM openjdk:11-jre-slim
RUN mkdir /app-config && mkdir /app
COPY --from=builder /usr/src/app/maestro-app/target/maestro-app-*.jar /app/maestro-app.jar
# allows consumers to override the application configs by mounting their configs here if they want.
# if nothing mounted the bundled configs will be used.
VOLUME /app-config
CMD ["java", "-jar", "/app/maestro-app.jar"]
EXPOSE 11235

