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

# Run as a non-root user. Create the avatar dir up front and hand /app to that
# user so the app can write uploads at runtime (no root, no mounted volume).
RUN useradd --system --uid 1001 spring \
 && mkdir -p /app/uploads/avatars \
 && chown -R spring:spring /app
USER spring

COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Avatars live under the app dir. NOTE: this disk is ephemeral on hosts without
# a mounted volume (Railway/Koyeb free) — uploads are wiped on every restart.
ENV AVATAR_DIR=/app/uploads/avatars

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
