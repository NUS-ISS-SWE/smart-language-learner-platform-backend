FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY target/sllp-evaluation-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
