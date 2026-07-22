#!/bin/bash
# Remove test-assets from the app's private sandbox storage.
source "$(dirname "$0")/common.sh"
adb_check

echo "Clearing sandbox assets: $SANDBOX_ASSETS"
adb shell "run-as $PACKAGE rm -rf $SANDBOX_ASSETS"
echo "Sandbox assets cleared."
