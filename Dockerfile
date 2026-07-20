# Etapa 1: Build con Maven + Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar

# Etapa 2: Runtime con Java 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/fleettrackpro-backend-1.0.0-SNAPSHOT-runner.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]