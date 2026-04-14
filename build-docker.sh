#!/bin/bash

# Build APK using Docker

set -e

echo "Building VRChat VRCA Downloader Android APK..."

# Build Docker image
docker build -t vrca-builder .

# Run build container
docker run --rm -v "$(pwd)/app/build:/app/app/build" vrca-builder

echo "Build completed!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
