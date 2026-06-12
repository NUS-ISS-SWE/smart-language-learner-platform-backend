# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy parent pom and user-service pom first for dependency caching
COPY pom.xml .
COPY sllp-user-service/pom.xml sllp-user-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-user-service/pom.xml \
    dependency:go-offline \
    -pl sllp-user-service \
    --also-make \
    -q 2>/dev/null || true

# Copy source and build
COPY sllp-user-service/src sllp-user-service/src
RUN mvn -f sllp-user-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-user-service/target/sllp-user-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
