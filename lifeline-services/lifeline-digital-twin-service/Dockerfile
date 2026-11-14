# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY lifeline-common ./lifeline-common
COPY lifeline-service-api ./lifeline-service-api
# Ensure Maven reactor can resolve all modules
COPY lifeline-gateway/pom.xml lifeline-gateway/pom.xml
COPY lifeline-mock/pom.xml lifeline-mock/pom.xml
COPY lifeline-bigdata/pom.xml lifeline-bigdata/pom.xml
RUN mkdir -p lifeline-bigdata/lifeline-flink-jobs lifeline-bigdata/lifeline-spark-jobs
COPY lifeline-bigdata/lifeline-flink-jobs/pom.xml lifeline-bigdata/lifeline-flink-jobs/pom.xml
COPY lifeline-bigdata/lifeline-spark-jobs/pom.xml lifeline-bigdata/lifeline-spark-jobs/pom.xml
COPY lifeline-services/pom.xml lifeline-services/pom.xml
COPY lifeline-services/lifeline-sys-auth-service/pom.xml lifeline-services/lifeline-sys-auth-service/pom.xml
COPY lifeline-services/lifeline-pipe-asset-service/pom.xml lifeline-services/lifeline-pipe-asset-service/pom.xml
COPY lifeline-services/lifeline-monitor-data-service/pom.xml lifeline-services/lifeline-monitor-data-service/pom.xml
COPY lifeline-services/lifeline-warning-service/pom.xml lifeline-services/lifeline-warning-service/pom.xml
COPY lifeline-services/lifeline-plan-service/pom.xml lifeline-services/lifeline-plan-service/pom.xml
COPY lifeline-services/lifeline-construction-service/pom.xml lifeline-services/lifeline-construction-service/pom.xml
COPY lifeline-services/lifeline-operation-service/pom.xml lifeline-services/lifeline-operation-service/pom.xml
COPY lifeline-services/lifeline-ai-gateway-service/pom.xml lifeline-services/lifeline-ai-gateway-service/pom.xml
COPY lifeline-services/lifeline-blockchain-service/pom.xml lifeline-services/lifeline-blockchain-service/pom.xml
COPY lifeline-services/lifeline-digital-twin-service/pom.xml lifeline-services/lifeline-digital-twin-service/pom.xml
# Copy actual source for the service being built
COPY lifeline-services/lifeline-digital-twin-service ./lifeline-services/lifeline-digital-twin-service
RUN mvn clean package -pl lifeline-services/lifeline-digital-twin-service -am -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="tianxin"
RUN addgroup -S lifeline && adduser -S lifeline -G lifeline
WORKDIR /app
COPY --from=builder /build/lifeline-services/lifeline-digital-twin-service/target/*.jar app.jar
USER lifeline
EXPOSE 9010
ENTRYPOINT ["java", "-jar", "app.jar"]
