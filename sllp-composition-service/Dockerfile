# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

# Copy parent pom and service pom first for dependency caching
COPY pom.xml .
COPY sllp-composition-service/pom.xml sllp-composition-service/pom.xml

# Download dependencies (best-effort, layer-cached)
RUN mvn -f sllp-composition-service/pom.xml \
    dependency:go-offline \
    -q 2>/dev/null || true

# Copy source and build
COPY sllp-composition-service/src sllp-composition-service/src
RUN mvn -f sllp-composition-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-composition-service/target/sllp-composition-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
