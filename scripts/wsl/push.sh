#!/bin/bash
set -e

# --- CONFIGURATION ---
APP_PACKAGE="com.example.app"
LOCAL_ASSETS_DIR="./app/src/main/assets/www"
ADB=/mnt/c/usr/bin/adb.exe

# Intermediate staging area natively readable by the unprivileged app user
LOCAL_TMP_DIR="/data/local/tmp/MyHybridMobileStagingMain"

echo "---------------------------------------------------"
echo "Instantly syncing local web assets over ADB..."
echo "---------------------------------------------------"

# 1. Verify that the Android asset source directory exists locally
if [ ! -d "$LOCAL_ASSETS_DIR" ]; then 
    echo "Error: Local asset folder not found at $LOCAL_ASSETS_DIR"
    exit 1 
fi

# 2. Rebuild the local temporary staging area on the device
echo "Preparing global staging area on device..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR' && mkdir -p '$LOCAL_TMP_DIR/www'"

# 3. Push local assets into the staging ground root folder
echo "Pushing native asset directory to staging ground..."
$ADB push "$LOCAL_ASSETS_DIR/." "$LOCAL_TMP_DIR/www" > /dev/null 2>&1

# 4. Enforce global access rules so run-as can read the folder
echo "Opening staging permissions for application context..."
$ADB shell "chmod -R 777 '$LOCAL_TMP_DIR'"

# 5. Clean, prepare, and copy the assets straight into the secure sandbox
echo "Deploying workspace assets to internal app sandbox..."
$ADB shell "run-as $APP_PACKAGE mkdir -p files"
$ADB shell "run-as $APP_PACKAGE rm -rf files/www"
$ADB shell "run-as $APP_PACKAGE cp -r '$LOCAL_TMP_DIR/www' files/"

# 6. Purge the temporary folder to free up device space
echo "Cleaning up temporary staging data..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR'"

# 7. Force a clear refresh on the WebView layout instantly by broadcasting a reload signal
echo "Sending reload broadcast to WebView layer..."
$ADB shell am broadcast -a "$APP_PACKAGE.ACTION_RELOAD_WEBVIEW" > /dev/null

echo "---------------------------------------------------"
echo "Sync Complete! Assets updated in real-time."
echo "---------------------------------------------------"

