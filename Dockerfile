# ── Build Stage ──
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy build files first (cached layer for dependencies)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# Copy source and build
COPY src/ src/
RUN gradle clean build -x test -x check --no-daemon

# ── Run Stage ──
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
