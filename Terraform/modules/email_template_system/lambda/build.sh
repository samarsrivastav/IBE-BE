#!/bin/bash
set -e  # Exit on error

echo "Starting Lambda function build process..."

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
npm ci --production

# Create a zip file
echo "Creating deployment package..."
zip -r ../function.zip .

# Clean up
echo "Cleaning up..."
cd ..
rm -rf build

echo "Lambda function packaged successfully as function.zip" 