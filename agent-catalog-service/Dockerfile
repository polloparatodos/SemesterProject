FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY config-service ./config-service
COPY agent-catalog-service ./agent-catalog-service
COPY deployment-service ./deployment-service
COPY customer-service ./customer-service

RUN mvn clean package -pl agent-catalog-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/agent-catalog-service/target/agent-catalog-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]