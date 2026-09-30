FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY sllp-contracts/pom.xml sllp-contracts/pom.xml
COPY sllp-learning-service/pom.xml sllp-learning-service/pom.xml
RUN mvn -f sllp-learning-service/pom.xml dependency:go-offline -q 2>/dev/null || true
COPY sllp-contracts/src sllp-contracts/src
COPY sllp-learning-service/src sllp-learning-service/src
RUN mvn -N install -DskipTests -q && mvn -f sllp-contracts/pom.xml install -DskipTests -q && mvn -f sllp-learning-service/pom.xml package -DskipTests -q

FROM azul/zulu-openjdk-alpine:25.0.2-25.32
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/sllp-learning-service/target/sllp-learning-service-*.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
