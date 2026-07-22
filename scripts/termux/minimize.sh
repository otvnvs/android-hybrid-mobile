#!/bin/bash
# Send the app to the background (press HOME).
source "$(dirname "$0")/common.sh"
adb_check

echo "Sending HOME key event..."
adb shell input keyevent KEYCODE_HOME
