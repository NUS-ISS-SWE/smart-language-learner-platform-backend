# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy parent pom and service pom first for dependency caching
COPY pom.xml .
COPY sllp-admin-service/pom.xml sllp-admin-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-admin-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Copy source and build
COPY sllp-admin-service/src sllp-admin-service/src
RUN mvn -f sllp-admin-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-admin-service/target/sllp-admin-service-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
