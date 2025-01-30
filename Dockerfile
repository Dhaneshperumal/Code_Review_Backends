# Use OpenJDK 17 image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml /app/
COPY src /app/src/

# Run the Maven build inside the container
RUN mvn clean package -DskipTests

# Copy the JAR file created by Maven to the container
COPY target/code_review-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application runs on (usually 8080 for Spring Boot)
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
