#!/bin/bash

# Exit on any error
set -e

# Check if the version is provided as a parameter
if [ -z "$1" ]; then
  echo "Usage: ./build.sh <version>"
  exit 1
fi

# Set variables
VERSION="$1"
PROJECT_NAME="webhook-dlq-service"
REGISTRY="longbale1999" # Replace with your Docker Hub username or registry
IMAGE_NAME="$REGISTRY/$PROJECT_NAME:$VERSION"

# Step 1: Clean and build the Spring Boot application
echo "Building the Spring Boot application..."
mvn clean package -DskipTests

# Step 2: Build the Docker image using the existing Dockerfile
echo "Building Docker image..."
docker build -t $IMAGE_NAME .

# Step 3: Push the Docker image to the registry
echo "Pushing Docker image to registry..."
docker push $IMAGE_NAME

echo "Docker image pushed successfully: $IMAGE_NAME"

# Step 4: Display completion message
echo "Build and deployment completed successfully!"
