#!/bin/bash

# Ensure we map the target directory correctly
APK_DIR="./app/build/outputs/apk/debug"
PORT=1234

echo "Starting darkhttpd to serve APK with Android MIME-type header..."
echo "Access your file at: http://<your-computer-ip>:$PORT/app-debug.apk"

# Run darkhttpd with the explicit Content-Type header override
darkhttpd "$APK_DIR" --port "$PORT" --header "Content-Type: application/vnd.android.package-archive"

