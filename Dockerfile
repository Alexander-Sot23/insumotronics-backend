# ====================== ETAPA 1: BUILD ======================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar Maven Wrapper y archivos de configuración
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos
RUN chmod +x ./mvnw

# Descargar dependencias (cache para builds más rápidos)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN ./mvnw clean package -DskipTests

# ====================== ETAPA 2: RUNTIME ======================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Crear usuario no root por seguridad
RUN useradd --create-home --shell /bin/bash appuser
USER appuser

# Copiar el JAR generado
COPY --from=build /app/target/insumotronics-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Configuraciones recomendadas para producción
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]