#!/bin/bash
DEVICE=$1
ADB=~/Android/platform-tools/adb

# Dynamically locate the directory where this script resides
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Safely find apks matching *app-debug.apk* without relying on grep
find ./ -type f -name "*app-debug.apk*" | while read -r APK; do
    # Extract package name safely
    PACKAGENAME=$(aapt dump badging "$APK" 2>/dev/null | grep package | awk '{print $2}' | sed "s/name=//g" | sed "s/'//g")
    
    if [ -z "$PACKAGENAME" ]; then
        echo "Could not extract package name from $APK"
        continue
    fi

    echo "starting $PACKAGENAME"
    
    if [ -z "$DEVICE" ]; then
        # Loop through all connected devices if no device is passed as an argument
        $ADB devices | grep -v attached | grep device | cut -f1 | while read -r TARGET_DEVICE; do
            echo $ADB -s "$TARGET_DEVICE" shell "monkey -p $PACKAGENAME -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1"
            
            # Call start.sh using the dynamically resolved portable path
            "$SCRIPT_DIR/start.sh" "$TARGET_DEVICE"
        done
    else
        echo "starting android application"
        $ADB -s "$DEVICE" shell "monkey -p $PACKAGENAME -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1"
    fi
done

