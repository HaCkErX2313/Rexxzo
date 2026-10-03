# ==========================================
# Stage 1: Build Spring Boot Application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install curl for robust downloading if needed by wrapper
RUN apk add --no-cache curl

# Copy Maven wrapper and POM first for layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix potential Windows CRLF line endings on mvnw and ensure execute permission
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies offline (cache layer)
RUN ./mvnw dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the production executable JAR (skipping tests for fast container builds)
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as a dedicated non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the compiled Spring Boot executable JAR
COPY --from=builder --chown=appuser:appgroup /app/target/rexxo-backend-*.jar app.jar

USER appuser

# Railway assigns port dynamically via $PORT; default to 8080 locally
EXPOSE 8080

# Configure container-aware JVM flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]