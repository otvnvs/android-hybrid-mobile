#!/bin/bash
# Force-stop the app.
source "$(dirname "$0")/common.sh"
adb_check

echo "Force-stopping $PACKAGE..."
adb shell am force-stop "$PACKAGE"
