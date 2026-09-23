FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY sllp-evidence-service/pom.xml sllp-evidence-service/pom.xml
COPY sllp-evidence-service/src sllp-evidence-service/src
RUN mvn -f sllp-evidence-service/pom.xml package -DskipTests -q

FROM azul/zulu-openjdk-alpine:25.0.2-25.32
WORKDIR /app
COPY --from=builder /build/sllp-evidence-service/target/sllp-evidence-service-*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
