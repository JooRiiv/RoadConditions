# Use Java 17 base image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the fat JAR into the container
COPY build/libs/ktor-backend-0.0.1.jar app.jar

# Expose the port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "app.jar"]
