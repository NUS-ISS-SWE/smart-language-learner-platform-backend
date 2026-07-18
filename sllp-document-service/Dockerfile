# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy parent pom and service pom first for dependency caching
COPY pom.xml .
COPY sllp-document-service/pom.xml sllp-document-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-document-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Copy source and build
COPY sllp-document-service/src sllp-document-service/src
RUN mvn -f sllp-document-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-document-service/target/sllp-document-service-*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
