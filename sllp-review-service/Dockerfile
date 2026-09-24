FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY sllp-review-service/pom.xml sllp-review-service/pom.xml
RUN mvn -f sllp-review-service/pom.xml dependency:go-offline -q 2>/dev/null || true
COPY sllp-review-service/src sllp-review-service/src
RUN mvn -f sllp-review-service/pom.xml package -DskipTests -q
FROM azul/zulu-openjdk-alpine:25.0.2-25.32
WORKDIR /app
COPY --from=builder /build/sllp-review-service/target/sllp-review-service-*.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "app.jar"]
