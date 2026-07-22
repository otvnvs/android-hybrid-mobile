#!/bin/bash
# Start the app's background service without bringing the UI to the foreground.
# Sets up the adb reverse tunnel so the service can reach the host test harness.

# Dynamically locate script folder and source common.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Define the WSL path to the Windows ADB executable
ADB="/mnt/c/usr/bin/adb.exe"

# If your common.sh depends on a global 'adb' command, you can export an alias or function
# Comment out or remove if adb_check handles it directly
adb_check() {
    if ! [ -x "$ADB" ]; then
        echo "Error: Windows ADB executable not found at $ADB" >&2
        exit 1
    fi
}
adb_check

HOST="127.0.0.1"
PORT="9999"
SETUP_REVERSE=1

while [[ $# -gt 0 ]]; do
    case "$1" in
        --host)
            HOST="$2"
            shift 2
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --no-reverse)
            SETUP_REVERSE=0
            shift
            ;;
        -h|--help)
            echo "Usage: $(basename "$0") [--host HOST] [--port PORT] [--no-reverse]"
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

# Ensure package name variable from common.sh is stripped of any hidden Windows line endings
PACKAGE=$(echo "$PACKAGE" | tr -d '\r')

if [[ "$SETUP_REVERSE" -eq 1 ]]; then
    echo "Setting up adb reverse tunnel tcp:${PORT} -> tcp:${PORT}..."
    # Execute using the WSL ADB path; strip \r from output if tracking return messages
    $ADB reverse "tcp:${PORT}" "tcp:${PORT}" || {
        echo "Warning: failed to set up adb reverse tunnel." >&2
    }
fi

echo "Starting background service for $PACKAGE (test harness ${HOST}:${PORT})..."
# am start-foreground-service often outputs lines with \r, handled cleanly here
$ADB shell am start-foreground-service \
    -n "$PACKAGE/com.example.app.services.background.BackgroundService" \
    --es "test_host" "$HOST" \
    --ei "test_port" "$PORT" | tr -d '\r'

