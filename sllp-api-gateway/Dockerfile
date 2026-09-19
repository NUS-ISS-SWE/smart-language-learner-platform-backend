# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy the parent, shared-library, and service POMs first for dependency caching.
# The gateway depends on the local sllp-contracts module.
COPY pom.xml .
COPY sllp-contracts/pom.xml sllp-contracts/pom.xml
COPY sllp-api-gateway/pom.xml sllp-api-gateway/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-api-gateway/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Install the shared library before packaging the service. The root POM includes
# modules that are intentionally not copied into this service-specific image.
COPY sllp-contracts/src sllp-contracts/src
COPY sllp-api-gateway/src sllp-api-gateway/src
RUN mvn -N install -DskipTests -q \
    && mvn -f sllp-contracts/pom.xml install -DskipTests -q \
    && mvn -f sllp-api-gateway/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-api-gateway/target/sllp-api-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
