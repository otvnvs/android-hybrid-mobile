#!/bin/bash
# Bring Termux to the foreground.
source "$(dirname "$0")/common.sh"
adb_check

echo "Focusing Termux..."
adb shell am start -n com.termux/.app.TermuxActivity
