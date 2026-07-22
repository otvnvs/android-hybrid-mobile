#!/bin/bash
# Copy test-assets into the app's private sandbox storage.
# Files are extracted as the app user via run-as to avoid root ownership issues.
source "$(dirname "$0")/common.sh"
adb_check

TMP_TAR="/data/local/tmp/$PACKAGE-test-assets.tar.gz"

echo "Packing local assets..."
cd "$LOCAL_ASSETS/.."
tar -czf /tmp/test-assets.tar.gz "$(basename "$LOCAL_ASSETS")"

echo "Pushing archive to device..."
adb push /tmp/test-assets.tar.gz "$TMP_TAR"

echo "Extracting into sandbox as app user..."
adb shell "run-as $PACKAGE sh -c 'mkdir -p files && rm -rf files/test-assets && tar -xzf $TMP_TAR -C files'"

echo "Cleaning up temporary archive..."
adb shell "rm -f $TMP_TAR"
rm -f /tmp/test-assets.tar.gz

echo "Sandbox copy complete."
