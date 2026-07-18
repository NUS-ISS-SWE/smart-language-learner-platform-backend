# ---- Build stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder

RUN apk add --no-cache maven

WORKDIR /build

COPY pom.xml .
COPY sllp-liquibase-service/pom.xml sllp-liquibase-service/pom.xml

RUN mvn -f sllp-liquibase-service/pom.xml dependency:go-offline -q 2>/dev/null || true

COPY sllp-liquibase-service/src sllp-liquibase-service/src
RUN mvn -f sllp-liquibase-service/pom.xml package -DskipTests -q

# ---- Runtime stage ----
FROM azul/zulu-openjdk-alpine:25.0.2-25.32

WORKDIR /app

COPY --from=builder /build/sllp-liquibase-service/target/sllp-liquibase-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
