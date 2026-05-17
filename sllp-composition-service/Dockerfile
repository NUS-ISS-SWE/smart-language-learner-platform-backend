FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY target/sllp-composition-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
