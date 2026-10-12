# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Se copian primero pom y wrapper para aprovechar la caché de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Las pruebas usan Testcontainers (necesitan Docker) y ya corren en GitHub Actions
RUN ./mvnw -B -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
