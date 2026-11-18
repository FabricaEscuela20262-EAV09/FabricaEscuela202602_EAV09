# Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline -B

COPY src ./src

# Compilar sin repetir pruebas
RUN ./mvnw package -DskipTests -B

# Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd -r -u 1001 spring

COPY --from=build --chown=spring:spring \
    /app/target/reservar-0.0.1-SNAPSHOT.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

