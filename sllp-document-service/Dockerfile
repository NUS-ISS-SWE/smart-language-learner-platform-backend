# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy the parent, shared-library, and service POMs first for dependency caching.
# The document service depends on the local sllp-contracts module.
COPY pom.xml .
COPY sllp-contracts/pom.xml sllp-contracts/pom.xml
COPY sllp-document-service/pom.xml sllp-document-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-document-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Install the shared library before packaging the service. The root POM includes
# modules that are intentionally not copied into this service-specific image.
COPY sllp-contracts/src sllp-contracts/src
COPY sllp-document-service/src sllp-document-service/src
RUN mvn -N install -DskipTests -q \
    && mvn -f sllp-contracts/pom.xml install -DskipTests -q \
    && mvn -f sllp-document-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-document-service/target/sllp-document-service-*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
