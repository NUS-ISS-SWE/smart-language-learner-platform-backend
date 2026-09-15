FROM azul/zulu-openjdk-alpine:25.0.2-25.32 AS builder
RUN apk add --no-cache maven
WORKDIR /build
COPY pom.xml .
COPY sllp-product-guide-service/pom.xml sllp-product-guide-service/pom.xml
COPY sllp-product-guide-service/src sllp-product-guide-service/src
RUN mvn -f sllp-product-guide-service/pom.xml package -DskipTests -q

FROM azul/zulu-openjdk-alpine:25.0.2-25.32
WORKDIR /app
COPY --from=builder /build/sllp-product-guide-service/target/sllp-product-guide-service-*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
