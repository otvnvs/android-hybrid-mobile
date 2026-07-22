#!/bin/bash

# --- CONFIGURATION ---
# Default emulator to use if NO --avd flag is passed to the script.
DEFAULT_AVD="Medium_Phone_API_35"
# ---------------------

# 1. Dynamically get Windows paths
WIN_USERPROFILE=$(wslvar USERPROFILE)
LINUX_USERPROFILE=$(wslpath "$WIN_USERPROFILE")

# 2. Define the path to the tools
SDK_PATH="$LINUX_USERPROFILE/AppData/Local/Android/Sdk"
EMULATOR_CMD="$SDK_PATH/emulator/emulator.exe"
ADB_CMD="/mnt/c/usr/bin/adb.exe" # Custom absolute Windows binary path

# 3. Check if emulator.exe actually exists (Skip this check only if purely killing processes)
if [ "$1" != "-k" ] && [ "$1" != "--kill" ] && [ ! -f "$EMULATOR_CMD" ]; then
    echo "ERROR: Android Emulator is not installed or could not be found at:"
    echo "       $EMULATOR_CMD"
    exit 1
fi

# 4. Initialize variables for arguments
AVD_NAME=""
STOP_MODE=false
KILL_MODE=false
COLD_BOOT=false
HEADLESS_MODE=false

# Helper function to print usage
show_help() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -a, --avd <name>    Specify the emulator name to run (Defaults to $DEFAULT_AVD)"
    echo "  -c, --cold          Force a cold boot (fixes black screen / stuck state)"
    echo "  -n, --headless      Run the emulator headlessly (no window UI)"
    echo "  -s, --stop          Stop the running emulator instance gracefully"
    echo "  -k, --kill          Force kill all emulator background processes immediately"
    echo "  -h, --help          Show this help screen"
    echo ""
    echo "Available emulators on your system:"
    "$EMULATOR_CMD" -list-avds | tr -d '\r'
    exit 1
}

# 5. Parse command line flags
while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--avd)
            if [[ -z "$2" || "$2" == -* ]]; then
                echo "ERROR: --avd requires an emulator name argument."
                exit 1
            fi
            AVD_NAME="$2"
            shift 2
            ;;
        -c|--cold)
            COLD_BOOT=true
            shift
            ;;
        -n|--headless)
            HEADLESS_MODE=true
            shift
            ;;
        -s|--stop)
            STOP_MODE=true
            shift
            ;;
        -k|--kill)
            KILL_MODE=true
            shift
            ;;
        -h|--help)
            show_help
            ;;
        *)
            echo "ERROR: Unknown option '$1'"
            echo "Use '$0 --help' for details."
            exit 1
            ;;
    esac
done

# ----------------------------------------------------
# ACTION: KILL MODE (Immediate Process Termination)
# ----------------------------------------------------
if [ "$KILL_MODE" = true ]; then
    echo "Forcefully terminating all emulator runtime processes..."
    /mnt/c/Windows/System32/taskkill.exe /f /im emulator.exe >/dev/null 2>&1
    /mnt/c/Windows/System32/taskkill.exe /f /im qemu-system* >/dev/null 2>&1
    echo "All processes force killed."
    exit 0
fi

# ----------------------------------------------------
# ACTION: STOP MODE (Graceful Snapshot Save)
# ----------------------------------------------------
if [ "$STOP_MODE" = true ]; then
    echo "Searching for active emulators via adb..."
    RUNNING_EMULATOR=$($ADB_CMD devices | grep "emulator-" | head -n 1 | awk '{print $1}')
    
    if [ -z "$RUNNING_EMULATOR" ]; then
        echo "No running emulators were detected."
        exit 0
    fi
    
    echo "Sending graceful shutdown request to ($RUNNING_EMULATOR)..."
    $ADB_CMD -s "$RUNNING_EMULATOR" emu kill
    exit 0
fi

# ----------------------------------------------------
# ACTION: START MODE
# ----------------------------------------------------
if [ -z "$AVD_NAME" ]; then
    AVD_NAME="$DEFAULT_AVD"
fi

if ! "$EMULATOR_CMD" -list-avds | tr -d '\r' | grep -qx "$AVD_NAME"; then
    echo "ERROR: Invalid emulator name '$AVD_NAME'."
    echo ""
    echo "Available emulators on your system:"
    "$EMULATOR_CMD" -list-avds | tr -d '\r'
    exit 1
fi

export ANDROID_HOME=$WIN_USERPROFILE

EMULATOR_ARGS=("-avd" "$AVD_NAME")

if [ "$COLD_BOOT" = true ]; then
    echo "Cold boot requested. Clearing old cache states..."
    EMULATOR_ARGS+=("-no-snapshot-load" "-feature" "WindowsHypervisorPlatform")
fi

if [ "$HEADLESS_MODE" = true ]; then
    echo "Headless mode requested. Disabling UI window..."
    EMULATOR_ARGS+=("-no-window")
fi

echo "Starting Android Emulator: $AVD_NAME..."
"$EMULATOR_CMD" "${EMULATOR_ARGS[@]}" > /dev/null 2>&1 &

echo "Emulator command sent. Run '$ADB_CMD devices' in a few seconds to verify."

