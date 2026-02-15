# ── Build Stage ──
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Fix Windows CRLF line endings and set execute permission
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Download dependencies first (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew clean build -x test -x check --no-daemon

# ── Run Stage ──
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
