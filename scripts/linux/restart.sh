#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$(dirname "$0")"

echo "Stopping application..."
"$SCRIPT_DIR/stop.sh"

echo "Starting application..."
"$SCRIPT_DIR/start.sh"

