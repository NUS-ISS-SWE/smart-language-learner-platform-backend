# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

COPY pom.xml .
COPY sllp-contracts/pom.xml sllp-contracts/pom.xml
COPY sllp-subscription-service/pom.xml sllp-subscription-service/pom.xml

RUN mvn -f sllp-subscription-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

COPY sllp-contracts/src sllp-contracts/src
COPY sllp-subscription-service/src sllp-subscription-service/src
RUN mvn -N install -DskipTests -q \
    && mvn -f sllp-contracts/pom.xml install -DskipTests -q \
    && mvn -f sllp-subscription-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-subscription-service/target/sllp-subscription-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
