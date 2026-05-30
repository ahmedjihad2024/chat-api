# ---- Build stage: compile the Spring Boot fat jar with the Gradle wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Warm the dependency cache first: copy only the wrapper + build scripts so this
# layer is reused unless the build config actually changes.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy the sources and build. Tests are skipped here — run them in CI, not
# in the image build (Twilio/Mongo wiring aside, it keeps deploys fast).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Run stage: a slim JRE with just the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

# Default avatar dir points at the mounted volume (see fly.toml [mounts]).
ENV AVATAR_DIR=/data/avatars

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
