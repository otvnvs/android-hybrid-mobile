#!/bin/bash
# Start the app's background service without bringing the UI to the foreground.
# Sets up the adb reverse tunnel so the service can reach the host test harness.
source "$(dirname "$0")/common.sh"
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

if [[ "$SETUP_REVERSE" -eq 1 ]]; then
    echo "Setting up adb reverse tunnel tcp:${PORT} -> tcp:${PORT}..."
    adb reverse "tcp:${PORT}" "tcp:${PORT}" || {
        echo "Warning: failed to set up adb reverse tunnel." >&2
    }
fi

echo "Starting background service for $PACKAGE (test harness ${HOST}:${PORT})..."
adb shell am start-foreground-service \
    -n "$PACKAGE/com.example.app.services.background.BackgroundService" \
    --es "test_host" "$HOST" \
    --ei "test_port" "$PORT"
