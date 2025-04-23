# Use a lightweight base image
FROM openjdk:24-ea-17-jdk

# Set the working directory
WORKDIR /app

# Copy the application JAR file
COPY target/dlq-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8088

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
