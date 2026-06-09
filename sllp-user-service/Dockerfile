FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY target/sllp-user-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
