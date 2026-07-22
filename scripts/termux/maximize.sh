#!/bin/bash
# Bring the app back to the foreground.
source "$(dirname "$0")/common.sh"
adb_check

echo "Bringing $PACKAGE to foreground..."
adb shell am start -n "$ACTIVITY"
