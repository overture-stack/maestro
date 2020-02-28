FROM openjdk:11-jdk as builder
WORKDIR /usr/src/app
COPY . .
# tests to be executed outside of dockerfile because we use testcontainers
# needs access to docker socket
RUN ./mvnw clean package -DskipTests

FROM openjdk:11-jre-slim
RUN mkdir /app
COPY --from=builder /usr/src/app/maestro-app/target/maestro-app-*.jar /app/maestro-app.jar

ENV APP_USER app_user
ENV APP_USER_ID 9999
ENV APP_USER_GROUP_ID 9999
ENV APP_DIR /app
RUN addgroup --system --gid $APP_USER_GROUP_ID $APP_USER \
    && adduser --system --uid $APP_USER_ID --gid $APP_USER_GROUP_ID $APP_USER  \
    && mkdir -p $APP_DIR \
    && chown -R $APP_USER:$APP_USER_GROUP_ID $APP_DIR
USER $APP_USER_ID

CMD ["java", "-jar", "/app/maestro-app.jar"]
EXPOSE 11235

