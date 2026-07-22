#!/bin/bash
# Copy test-assets into the public storage workspace.
source "$(dirname "$0")/common.sh"
adb_check

echo "Copying test assets to public storage: $PUBLIC_ASSETS"
adb shell "mkdir -p $PUBLIC_ASSETS"
adb push "$LOCAL_ASSETS/" "$PUBLIC_ASSETS/"
echo "Public copy complete."
