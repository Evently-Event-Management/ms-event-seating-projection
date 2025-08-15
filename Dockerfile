# --- Build Stage ---
# Use a Maven image to build the application JAR. This keeps our final image small.
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests for faster builds in this context
RUN mvn clean package -DskipTests

# --- Final Stage ---
# Use a minimal, secure Java base image
FROM openjdk:21-slim

# Set the working directory
WORKDIR /app

# Copy only the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set the command to run the application when the container starts
ENTRYPOINT ["java","-jar","/app/app.jar"]
