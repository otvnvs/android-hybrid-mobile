#!/bin/bash
# Common configuration for AHM helper scripts.

set -e

export PACKAGE="com.example.app"
export ACTIVITY="$PACKAGE/.MainActivity"

# Sandbox paths (app private storage)
export SANDBOX_DIR="/data/data/$PACKAGE/files"
export SANDBOX_ASSETS="$SANDBOX_DIR/test-assets"

# Public storage workspace (must match the app's configured workspace)
export PUBLIC_DIR="/sdcard/Documents/MyHybridMobile/www"
export PUBLIC_ASSETS="$PUBLIC_DIR/test-assets"

# Local test asset source
export LOCAL_ASSETS="$(cd "$(dirname "$0")/.." && pwd)/test-assets"

# ADB helper that aborts if adb is not available.
adb_check() {
    if ! command -v adb >/dev/null 2>&1; then
        echo "Error: adb not found in PATH." >&2
        exit 1
    fi
}
