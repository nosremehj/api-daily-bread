# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
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
# Imagem pensada para deploy: perfil prod (PostgreSQL). Local com H2: mvn spring-boot:run.
ENV SPRING_PROFILES_ACTIVE=prod
# Em docker-compose, sobrescreva com jdbc:postgresql://postgres:5432/bread (nome do serviço).
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
