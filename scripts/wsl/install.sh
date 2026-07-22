#!/bin/bash

# Default configuration
ADB=/mnt/c/usr/bin/adb.exe
DEVICE=""
GRANT_FLAG=""

# Function to display help menu
show_help() {
    echo "Usage: $(basename "$0") [options] [device_id]"
    echo ""
    echo "Options:"
    echo "  --grant all    Grant all permissions during installation (-g)"
    echo "  --grant none   Do not grant all permissions (default)"
    echo "  --help         Show this help message and exit"
    echo ""
    echo "Arguments:"
    echo "  device_id      Optional. Specific Android device ID to target."
    exit 0
}

# Parse command line options
while [[ $# -gt 0 ]]; do
    case "$1" in
        --help)
            show_help
            ;;
        --grant)
            if [ "$2" == "all" ]; then
                GRANT_FLAG="-g"
                shift 2
            elif [ "$2" == "none" ]; then
                GRANT_FLAG=""
                shift 2
            else
                echo "Error: Invalid argument for --grant. Use 'all' or 'none'."
                exit 1
            fi
            ;;
        -*)
            echo "Error: Unknown option $1"
            echo "Use --help for usage details."
            exit 1
            ;;
        *)
            # If it doesn't start with a dash, treat it as the DEVICE ID
            DEVICE="$1"
            shift
            ;;
    esac
done

# Find and install the APKs
find ./ | grep "app-debug.apk" | while read -r APK; do
    echo "Installing $APK"
    if [ -z "$DEVICE" ]; then
        # Recursively call this script for all connected devices, passing down the grant flag
        $ADB devices | grep -v attached | grep device | cut -f1 | while read -r DETECTED_DEVICE; do
            # Pass the grant argument if it was originally set
            if [ -n "$GRANT_FLAG" ]; then
                "$0" --grant all "$DETECTED_DEVICE"
            else
                "$0" --grant none "$DETECTED_DEVICE"
            fi
        done
    else
        if [ "$GRANT_FLAG" == "-g" ]; then
            echo "Installing android application and granting all permissions..."
            $ADB -s "$DEVICE" install -g "$APK"
        else
            echo "Installing android application without granting all permissions (use --grant all to grant them)..."
            $ADB -s "$DEVICE" install "$APK"
        fi
    fi
done

