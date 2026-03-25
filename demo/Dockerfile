# ============================================================
# STAGE 1: BUILD
# Usamos una imagen con Maven y JDK para compilar el proyecto.
# "builder" es solo un alias interno, no genera imagen final.
# ============================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiamos primero el pom.xml y descargamos dependencias.
# Truco: si el código cambia pero el pom.xml no, Docker usa
# la capa de cache y no vuelve a descargar todas las deps.
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

# Ahora copiamos el código fuente y compilamos el JAR
COPY src ./src
RUN mvn clean package -DskipTests -q

# ============================================================
# STAGE 2: RUNTIME
# Imagen final mucho más ligera — solo necesita el JRE.
# La imagen de build pesaba ~600MB; esta pesa ~200MB.
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos SOLO el JAR del stage anterior (no el JDK, no Maven)
COPY --from=builder /app/target/*.jar app.jar

# El puerto que expone el servidor Spring (HTTPS)
EXPOSE 8443

# Variables de entorno con valores por defecto (sobreescribibles al lanzar el contenedor)
ENV SSL_KEYSTORE_PASSWORD=changeit
ENV API_KEY=sk_test-123456789

# Comando de arranque con una JVM ajustada para contenedores:
# -XX:+UseContainerSupport: respeta los límites de memoria de Docker
# -XX:MaxRAMPercentage=75: usa máximo 75% de la RAM asignada al container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
