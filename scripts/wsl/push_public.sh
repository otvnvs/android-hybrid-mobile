#!/bin/bash
set -e

# --- CONFIGURATION ---
APP_PACKAGE="com.example.app"
LOCAL_ASSETS_DIR="./app/src/main/assets/www"
TARGET_DIR="/sdcard/Documents/MyHybridMobile"
ADB=/mnt/c/usr/bin/adb.exe

# Intermediate staging area natively readable by the unprivileged app user
LOCAL_TMP_DIR="/data/local/tmp/MyHybridMobileStagingPublic"

echo "---------------------------------------------------"
echo "Instantly syncing native assets via Sandbox & SDCard..."
echo "---------------------------------------------------"

# 1. Verify that the Android asset source directory exists locally
if [ ! -d "$LOCAL_ASSETS_DIR" ]; then 
    echo "Error: Local asset folder not found at $LOCAL_ASSETS_DIR"
    exit 1 
fi

# 2. Clear out and rebuild the local temporary staging and public areas
echo "Preparing staging and public workspaces on device..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR' && mkdir -p '$LOCAL_TMP_DIR/www'"
$ADB shell "rm -rf '$TARGET_DIR' && mkdir -p '$TARGET_DIR/www'"

# 3. Push local assets into the staging ground root folder
echo "Pushing native asset directory to staging ground..."
$ADB push "$LOCAL_ASSETS_DIR/." "$LOCAL_TMP_DIR/www" > /dev/null 2>&1

# 4. Enforce global access rules so run-as can read the folder
echo "Opening staging permissions for application context..."
$ADB shell "chmod -R 777 '$LOCAL_TMP_DIR'"

# 5. Deploy everything inside the secure app sandbox using 'run-as'
echo "Deploying to secure application sandbox..."
$ADB shell "run-as $APP_PACKAGE mkdir -p files"
$ADB shell "run-as $APP_PACKAGE rm -rf files/www"
$ADB shell "run-as $APP_PACKAGE cp -r '$LOCAL_TMP_DIR/www' files/"

# 6. Mirror clean assets out to public SDCard using high-privilege shell access
echo "Mirroring assets out to public SDCard workspace..."
$ADB shell "cp -r '$LOCAL_TMP_DIR/www/.' '$TARGET_DIR/www/'"

# 7. Purge the global temporary folder to avoid cluttering up device space
echo "Cleaning up temporary staging data..."
$ADB shell "rm -rf '$LOCAL_TMP_DIR'"

# 8. Signal the active WebView layer to execute a live reload interface transition
echo "Sending reload broadcast to WebView layer..."
$ADB shell am broadcast -a "$APP_PACKAGE.ACTION_RELOAD_WEBVIEW" > /dev/null

echo "---------------------------------------------------"
echo "Sync Complete! Build assets updated inside public staging lane."
echo "---------------------------------------------------"

