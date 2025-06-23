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

# Copy the built JAR from build stage
COPY --from=build /app/target/ChessMystic-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "app.jar"]
