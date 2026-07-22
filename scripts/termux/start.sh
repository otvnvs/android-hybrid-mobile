#!/bin/bash
# Start the app on the connected device.
source "$(dirname "$0")/common.sh"
adb_check

echo "Starting $ACTIVITY..."
adb shell am start -n "$ACTIVITY"
