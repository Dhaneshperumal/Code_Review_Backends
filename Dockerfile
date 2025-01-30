# Use a base image with OpenJDK
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the host machine to the container
COPY target/code_review-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application runs on (usually 8080 for Spring Boot)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
