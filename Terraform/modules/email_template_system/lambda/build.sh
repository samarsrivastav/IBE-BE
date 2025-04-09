#!/bin/bash

# Create a temporary directory for the build
rm -rf build
mkdir -p build

# Copy the Lambda function code and package.json
cp index.js package.json build/

# Install dependencies
cd build
npm install --production

# Create a zip file
zip -r ../function.zip .

# Clean up
cd ..
rm -rf build

echo "Lambda function packaged successfully as function.zip" 