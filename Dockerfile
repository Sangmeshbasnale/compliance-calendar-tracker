# ── Stage 1: Build ───────────────────────────────────────────────
# Java 25 JDK on Alpine — matches pom.xml compiler release
FROM eclipse-temurin:25-jdk-alpine AS builder

# Install Maven (no mvnw in this project)
RUN apk add --no-cache maven

WORKDIR /build

# Copy pom first — Docker cache skips dependency download on source-only changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and package, skipping tests (run separately via Testcontainers)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
# Slim JRE — no compiler, no Maven, smaller attack surface
FROM eclipse-temurin:25-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Container-aware JVM flags:
#   UseContainerSupport  — honour cgroup CPU/memory limits
#   MaxRAMPercentage     — use 75 % of container RAM for heap
#   urandom              — avoid /dev/random blocking on startup
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
