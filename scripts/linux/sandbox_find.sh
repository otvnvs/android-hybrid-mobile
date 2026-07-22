#!/bin/bash
DEVICE=$1
ADB=~/Android/platform-tools/adb

# Attempt to find the target APK in the current working tree
APK_PATH=$(find ./ | grep "app-debug.apk" | head -n 1)

if [ -z "$APK_PATH" ]; then
    echo "Error: Could not locate 'app-debug.apk' in the current directories."
    exit 1
fi

# Ensure the aapt tool is accessible to extract the package identity
if ! command -v aapt &> /dev/null; then
    echo "Error: 'aapt' utility is not installed or missing from your PATH."
    echo "Please install it via 'sudo apt install aapt'."
    exit 1
fi

# Parse out the unique application package name from the APK file structure
PACKAGE_NAME=$(aapt dump badging "$APK_PATH" | grep "package" | sed -e "s/package: name='//" -e "s/'.*//")

if [ -z "$PACKAGE_NAME" ]; then
    echo "Error: Failed to safely parse the package identifier out of $APK_PATH."
    exit 1
fi

echo "Found application package: $PACKAGE_NAME"

# Evaluate target system execution parameters
if [ -z "$DEVICE" ]
then
    echo "No target device argument defined. Broadly sweeping connected ADB targets..."
    $ADB devices | grep -v attached | grep device | cut -f1 | while read -r CONNECTED_DEVICE; do
        echo "Listing sandbox filesystem on device: $CONNECTED_DEVICE"
        # Switched to 'ls -laR' for detailed long-listing format including hidden files
        $ADB -s "$CONNECTED_DEVICE" shell "run-as $PACKAGE_NAME find ./"
    done
else
    echo "Listing sandbox filesystem on explicitly targeted device: $DEVICE"
    # Switched to 'ls -laR' for detailed long-listing format including hidden files
    $ADB -s "$DEVICE" shell "run-as $PACKAGE_NAME find ./"
fi

