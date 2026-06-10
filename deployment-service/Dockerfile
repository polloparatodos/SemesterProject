FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY config-service ./config-service
COPY agent-catalog-service ./agent-catalog-service
COPY deployment-service ./deployment-service
COPY customer-service ./customer-service

RUN mvn clean package -pl deployment-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/deployment-service/target/deployment-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]