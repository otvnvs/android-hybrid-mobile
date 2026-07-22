#!/bin/bash
ADB="/mnt/c/usr/bin/adb.exe"
PACKAGE="com.example.app"

# 1. Fetch the process ID from the device
# 2. Strip Windows carriage returns (\r) and trailing newlines using tr and xargs
PID=$($ADB shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | xargs)

if [ -z "$PID" ]; then
    echo "Error: Application '$PACKAGE' is not currently running on the device." >&2
    exit 1
fi

echo "Found PID $PID for $PACKAGE. Starting logcat..."

# Run logcat filtered by the clean PID string
$ADB logcat --pid="$PID"

