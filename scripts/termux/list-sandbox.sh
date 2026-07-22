#!/bin/bash
# List all files under the app's sandbox test-assets directory using find.
source "$(dirname "$0")/common.sh"
adb_check

echo "Listing sandbox files: $SANDBOX_ASSETS"
adb shell "run-as $PACKAGE find $SANDBOX_ASSETS -type f -o -type d | sort" || true
