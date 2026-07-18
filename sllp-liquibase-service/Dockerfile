FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY target/sllp-liquibase-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
