# ====================== ETAPA 1: BUILD (compilación) ======================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar Maven Wrapper y pom.xml primero (para aprovechar caché de dependencias)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos al wrapper
RUN chmod +x ./mvnw

# Descargar dependencias (esto se cachea si el pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Construir el JAR (saltamos tests para que sea más rápido en Docker)
RUN ./mvnw clean package -DskipTests

# ====================== ETAPA 2: RUNTIME (imagen ligera) ======================
FROM eclipse-temurin:17-jre-jammy

# Crear un usuario no-root por seguridad (buena práctica)
RUN useradd --create-home --shell /bin/bash appuser
USER appuser

WORKDIR /app

# Copiar solo el JAR generado de la etapa de build
COPY --from=build /app/target/insumotronics-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto (Spring Boot por defecto usa 8080)
EXPOSE 8080

# Variables de entorno recomendadas
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]