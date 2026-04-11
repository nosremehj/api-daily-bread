# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY mvnw mvnw.cmd .mvn/ ./
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring
WORKDIR /app
COPY --from=build /build/target/bread-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV PORT=8080
# Render define PORT; application.properties usa ${PORT:9090}
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
