#!/bin/bash
DEVICE=""
ACTION="" # Will hold "ADD" or "REMOVE"
TARGET_ALL=false
SPECIFIC_PERMISSIONS=()
ADB=/mnt/c/usr/bin/adb.exe

# 1. Visual Command Line Guide Menu
show_help() {
    echo "========================================================================="
    echo " Android Hybrid Container Permission Management Engine"
    echo "========================================================================="
    echo "Usage Layout:"
    echo "  $0 --add --all"
    echo "  $0 --add [permission1] [permission2] ..."
    echo "  $0 --remove --all"
    echo "  $0 --remove [permission1] [permission2] ..."
    echo "  $0 --help"
    echo ""
    echo "Supported Permissions Keywords (Case-Insensitive):"
    echo "  camera, record_audio, read_external_storage, manage_external_storage"
    echo "========================================================================="
}

# 2. Argument Parsing Core Engine Loop
while [[ $# -gt 0 ]]; do
    case "$1" in
        --help|-h)
            show_help
            exit 0
            ;;
        --remove)
            ACTION="REMOVE"
            shift
            ;;
        --add)
            ACTION="ADD"
            shift
            ;;
        --all)
            TARGET_ALL=true
            shift
            ;;
        *)
            # If it's not a flag, treat it as a specific permission target token
            SPECIFIC_PERMISSIONS+=("$1")
            shift
            ;;
    esac
done

# Validation Guard: Make sure an action was requested if not running help menu
if [ -z "$ACTION" ]; then
    echo "Error: You must specify either --add or --remove."
    show_help
    exit 1
fi

# Fallback Default List if --all flag is passed manually
ALL_APP_PERMISSIONS=("CAMERA" "RECORD_AUDIO" "READ_EXTERNAL_STORAGE" "MANAGE_EXTERNAL_STORAGE")
if [ "$TARGET_ALL" = true ]; then
    SPECIFIC_PERMISSIONS=("${ALL_APP_PERMISSIONS[@]}")
fi

# 3. Find target APK and handle the adb loop logic executions
find ./ | grep "app-debug.apk" | while read APK; do
    PACKAGENAME=$(aapt dump badging "$APK" | grep package | awk '{print $2}' | sed s/name=//g | sed s/\'//g)
    
    # Auto-discover target device connectivity paths via ADB tools
    $ADB devices | grep -v attached | grep device | cut -f1 | while read DETECTED_DEVICE; do
        
        echo "Processing changes for $PACKAGENAME on target device: $DETECTED_DEVICE"
        
        # Special Global Handling Case for Resetting Everything 
        if [ "$ACTION" == "REMOVE" ] && [ "$TARGET_ALL" = true ]; then
            echo " -> Executing a clean global permission reset dump..."
            $ADB -s "$DETECTED_DEVICE" shell pm reset-permissions "$PACKAGENAME"
            continue
        fi

        # Process permission tokens sequentially
        for PERM in "${SPECIFIC_PERMISSIONS[@]}"; do
            UPPER_PERM=$(echo "$PERM" | tr '[:lower:]' '[:upper:]')
            CLEAN_PERM=${UPPER_PERM#ANDROID.PERMISSION.}
            FULL_PERM_STRING="android.permission.$CLEAN_PERM"

            if [ "$ACTION" == "ADD" ]; then
                echo " -> Granting: $FULL_PERM_STRING"
                # Manage Special All Files Manager switch vs standard runtime privileges
                if [ "$CLEAN_PERM" == "MANAGE_EXTERNAL_STORAGE" ]; then
                    $ADB -s "$DETECTED_DEVICE" shell appops set "$PACKAGENAME" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null
                else
                    $ADB -s "$DETECTED_DEVICE" shell pm grant "$PACKAGENAME" "$FULL_PERM_STRING" 2>/dev/null
                fi
            elif [ "$ACTION" == "REMOVE" ]; then
                echo " -> Revoking: $FULL_PERM_STRING"
                if [ "$CLEAN_PERM" == "MANAGE_EXTERNAL_STORAGE" ]; then
                    $ADB -s "$DETECTED_DEVICE" shell appops set "$PACKAGENAME" MANAGE_EXTERNAL_STORAGE deny 2>/dev/null
                else
                    $ADB -s "$DETECTED_DEVICE" shell pm revoke "$PACKAGENAME" "$FULL_PERM_STRING" 2>/dev/null
                fi
            fi
        done

        # Cold-kill application context process tracking frames to force configuration updates
        $ADB -s "$DETECTED_DEVICE" shell am force-stop "$PACKAGENAME"
        echo "Updates complete. Application state synchronized successfully."
    done
done

