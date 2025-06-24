# Backend Dockerfile
# First stage: dependency resolution
FROM maven:3.9.6-eclipse-temurin-21 AS dependencies

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Second stage: build application
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy dependencies from previous stage
COPY --from=dependencies /root/.m2 /root/.m2

# Copy project files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Make mvnw executable
RUN chmod +x mvnw

# Build the application (dependencies should be cached)
RUN ./mvnw clean package -DskipTests -B

# Final runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from build stage
COPY --from=build /app/target/ChessMystic-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application with proper JVM options for containerized environment
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
