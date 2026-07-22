#!/bin/bash
# Install the debug APK on the connected device.
source "$(dirname "$0")/common.sh"
adb_check

cd "$(dirname "$0")/.."
APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "Error: $APK not found. Run scripts/build.sh first." >&2
    exit 1
fi

echo "Installing $APK..."
adb install -r "$APK"
