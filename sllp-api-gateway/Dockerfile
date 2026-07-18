# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy parent pom and service pom first for dependency caching
COPY pom.xml .
COPY sllp-api-gateway/pom.xml sllp-api-gateway/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-api-gateway/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Copy source and build
COPY sllp-api-gateway/src sllp-api-gateway/src
RUN mvn -f sllp-api-gateway/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-api-gateway/target/sllp-api-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
