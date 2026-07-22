#!/bin/bash
# Uninstall the app from the connected device.
source "$(dirname "$0")/common.sh"
adb_check

echo "Uninstalling $PACKAGE..."
adb uninstall "$PACKAGE"
