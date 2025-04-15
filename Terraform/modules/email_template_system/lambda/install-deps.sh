#!/bin/bash
set -e  # Exit on error

echo "Installing dependencies for Lambda function..."

# Create a temporary directory for the build
echo "Creating build directory..."
rm -rf build
mkdir -p build

# Copy the Lambda function code and package files
echo "Copying source files..."
cp index.js package.json package-lock.json build/

# Install dependencies
echo "Installing dependencies..."
cd build
npm install --production

# Create a zip file
echo "Creating deployment package..."
zip -r ../function.zip .

# Clean up
echo "Cleaning up..."
cd ..
rm -rf build

echo "Deployment package created successfully!" 