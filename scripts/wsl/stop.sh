#!/bin/bash
DEVICE=$1
ADB=/mnt/c/usr/bin/adb.exe

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

    echo "stopping $PACKAGENAME"
    
    if [ -z "$DEVICE" ]; then
        # Loop through all connected devices if no device is passed as an argument
        $ADB devices | grep -v attached | grep device | cut -f1 | while read -r TARGET_DEVICE; do
            # Call stop.sh using the dynamically resolved portable path
            "$SCRIPT_DIR/stop.sh" "$TARGET_DEVICE"
        done
    else
        echo "stopping android application"
        $ADB -s "$DEVICE" shell "am force-stop $PACKAGENAME"
    fi
done

