#!/bin/bash
# List all files under the public storage test-assets directory using find.
source "$(dirname "$0")/common.sh"
adb_check

echo "Listing public files: $PUBLIC_ASSETS"
adb shell "find $PUBLIC_ASSETS -type f -o -type d | sort" || true
