#!/bin/bash
set -e

# --- CONFIGURATION ---
APP_PACKAGE="com.example.app"
LOCAL_ASSETS_DIR="./app/src/main/assets/www"
ADB=~/Android/platform-tools/adb

# Intermediate staging area natively readable by the unprivileged app user
LOCAL_TMP_DIR="/data/local/tmp/MyHybridMobileStagingSandbox"

echo "---------------------------------------------------"
echo "Pushing native assets directly to private sandbox..."
echo "---------------------------------------------------"

# 1. Verify that the Android asset source directory exists locally
if [ ! -d "$LOCAL_ASSETS_DIR" ]; then 
    echo "Error: Local asset folder not found at $LOCAL_ASSETS_DIR"
    exit 1 
fi

# 2. Clear out and rebuild the local temporary staging area on the device
echo "Preparing temporary staging area on device..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR' && mkdir -p '$LOCAL_TMP_DIR/www'"

# 3. Push local assets into the staging ground root folder
echo "Pushing native asset directory to staging ground..."
$ADB push "$LOCAL_ASSETS_DIR/." "$LOCAL_TMP_DIR/www" > /dev/null 2>&1

# 4. Enforce global access rules on our temporary staging area so run-as can read it
echo "Opening staging permissions for application context..."
$ADB shell "chmod -R 777 '$LOCAL_TMP_DIR'"

# 5. Clean, prepare, and copy the assets straight into the secure sandbox
echo "Deploying from staging ground directly into secure sandbox..."
$ADB shell "run-as $APP_PACKAGE mkdir -p files"
$ADB shell "run-as $APP_PACKAGE rm -rf files/www"
$ADB shell "run-as $APP_PACKAGE cp -r '$LOCAL_TMP_DIR/www' files/"

# 6. Purge the global temporary folder to avoid cluttering up device space
echo "Cleaning up temporary staging data..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR'"

# 7. Correct permissions inside the private folder to guarantee WebView readability
echo "Enforcing read/write permissions on sandbox assets..."
$ADB shell "run-as $APP_PACKAGE chmod -R 777 files/www"

# 8. Signal the active WebView layer to execute a live reload interface transition
echo "Sending reload broadcast to WebView layer..."
$ADB shell am broadcast -a "$APP_PACKAGE.ACTION_RELOAD_WEBVIEW" > /dev/null

echo "---------------------------------------------------"
echo "Direct Sandbox Sync Complete!"
echo "---------------------------------------------------"

