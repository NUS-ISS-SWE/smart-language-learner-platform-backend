FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY target/sllp-document-service-*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
