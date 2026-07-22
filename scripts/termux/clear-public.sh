#!/bin/bash
# Remove test-assets from the public storage workspace.
source "$(dirname "$0")/common.sh"
adb_check

echo "Clearing public assets: $PUBLIC_ASSETS"
adb shell "rm -rf $PUBLIC_ASSETS"
echo "Public assets cleared."
