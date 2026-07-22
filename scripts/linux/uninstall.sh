#!/bin/bash
DEVICE=$1
ADB=~/Android/platform-tools/adb

# Dynamically locate the directory where this script resides
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE}")" && pwd)"

# Safely find apks matching *app-debug.apk* without relying on grep
find ./ -type f -name "*app-debug.apk*" | while read -r APK; do
    echo "Processing $APK"
    
    # Stripped \r from aapt output to ensure clean package variable
    PACKAGENAME=$(aapt dump badging "$APK" 2>/dev/null | grep package | awk '{print $2}' | sed "s/name=//g" | sed "s/'//g" | tr -d '\r')

    if [ -z "$PACKAGENAME" ]; then
        echo "Could not extract package name from $APK. Skipping..."
        continue
    fi

    if [ -z "$DEVICE" ]; then
        # Loop through all connected devices if no device is passed as an argument
        $ADB devices | grep -v attached | grep device | cut -f1 | while read -r TARGET_DEVICE; do
            # Call uninstall.sh using the dynamically resolved portable path
            "$SCRIPT_DIR/uninstall.sh" "$TARGET_DEVICE"
        done
    else
        # Check if installed, piping through tr -d '\r' to strip Windows line endings
        IS_INSTALLED=$($ADB -s "$DEVICE" shell pm list packages "$PACKAGENAME" 2>/dev/null | tr -d '\r' | grep "^package:$PACKAGENAME$")

        if [ -n "$IS_INSTALLED" ]; then
            echo "Purging all remaining app data for $PACKAGENAME..."
            $ADB -s "$DEVICE" shell pm clear "$PACKAGENAME" > /dev/null 2>&1

            echo "Uninstalling android application: $PACKAGENAME"
            $ADB -s "$DEVICE" uninstall "$PACKAGENAME" > /dev/null 2>&1
        else
            echo "Application $PACKAGENAME is not installed on device $DEVICE. Skipping..."
        fi
    fi
done

