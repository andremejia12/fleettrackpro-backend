# Build: 2026-07-20
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/fleettrackpro-backend-1.0.0-SNAPSHOT-runner.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx400m", "-jar", "./app.jar"]