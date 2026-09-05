# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy the parent, shared-library, and service POMs first for dependency caching.
# sllp-evaluation-service depends on the local sllp-contracts reactor module.
COPY pom.xml .
COPY sllp-contracts/pom.xml sllp-contracts/pom.xml
COPY sllp-evaluation-service/pom.xml sllp-evaluation-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-evaluation-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Install the shared library in the builder's local Maven repository before
# packaging the service. The root POM contains other modules that are not
# copied into this service-specific image build.
COPY sllp-contracts/src sllp-contracts/src
COPY sllp-evaluation-service/src sllp-evaluation-service/src
RUN mvn -N install -DskipTests -q \
    && mvn -f sllp-contracts/pom.xml install -DskipTests -q \
    && mvn -f sllp-evaluation-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-evaluation-service/target/sllp-evaluation-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
