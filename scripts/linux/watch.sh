#!/bin/bash

# Target observation coordinates
WEB_ASSETS_DIR="./app/src/main/assets"
JAVA_SCRATCH_DIR="./app/src/main/java/com/example/app"

# Application Package & Remote Paths
APP_PACKAGE="com.example.app"
TARGET_WORKSPACE_BASE="/sdcard/Documents/MyHybridMobile"
TARGET_SANDBOX_BASE="files/www"

# Executable Path Binaries
ADB=~/Android/platform-tools/adb

# ANSI Color Codes
CLR_RESET="\033[0m"
CLR_INFO="\033[1;34m"    # Bold Blue
CLR_SUCCESS="\033[1;32m" # Bold Green
CLR_WARN="\033[1;33m"    # Bold Yellow
CLR_ERROR="\033[1;31m"   # Bold Red

echo "==================================================="
echo -e "${CLR_INFO}UNIVERSAL MONOLITHIC WORKSPACE DAEMON ACTIVE${CLR_RESET}"
echo "==================================================="
echo "Monitoring Web Assets:  $WEB_ASSETS_DIR"
echo "Monitoring Java Slices: $JAVA_SCRATCH_DIR"
echo "Press [CTRL+C] to terminate the live monitoring loop."
echo "---------------------------------------------------"

# Verification pre-flight checks
if [ ! -d "$WEB_ASSETS_DIR" ] || [ ! -d "$JAVA_SCRATCH_DIR" ]; then
    echo -e "${CLR_ERROR}Error: Target observation directories are missing.${CLR_RESET}"
    exit 1
fi

# Trackers mapping file paths to timestamps
declare -A WEB_FILE_TIMESTAMPS
declare -A JAVA_FILE_TIMESTAMPS

# Initialize base timestamps for Web Assets (Ignoring Vim swap tracks)
for f in $(find "$WEB_ASSETS_DIR" -type f -not -name ".*.swp"); do
    WEB_FILE_TIMESTAMPS["$f"]=$(stat -c %Y "$f")
done

# Initialize base timestamps for Java files
for f in $(find "$JAVA_SCRATCH_DIR" -name "*.java"); do
    JAVA_FILE_TIMESTAMPS["$f"]=$(stat -c %Y "$f")
done

while true; do
    # ---------------------------------------------------------
    # INTERCEPT VECTOR A: WEB ASSETS DELTA TRACKING & PUSH
    # ---------------------------------------------------------
    for current_file in $(find "$WEB_ASSETS_DIR" -type f -not -name ".*.swp"); do
        current_time=$(stat -c %Y "$current_file" 2>/dev/null || echo 0)
        last_time=${WEB_FILE_TIMESTAMPS["$current_file"]}

        if [ -z "$last_time" ] || [ "$current_time" -gt "$last_time" ]; then
            WEB_FILE_TIMESTAMPS["$current_file"]=$current_time
            
            if [ ! -z "$last_time" ]; then
                # Isolate the structural relative path context
                RELATIVE_PATH="${current_file#$WEB_ASSETS_DIR/}"
                
                echo -e "\n${CLR_SUCCESS}[ASSET CHANGE DETECTED] Delta Syncing: $RELATIVE_PATH${CLR_RESET}"
                
                # Resolve precise nested remote file target destinations
                REMOTE_WORKSPACE_FILE="$TARGET_WORKSPACE_BASE/$RELATIVE_PATH"
                REMOTE_SANDBOX_FILE="$TARGET_SANDBOX_BASE/$RELATIVE_PATH"
                
                # Isolate parent directory boundaries
                REMOTE_WORKSPACE_DIR=$(dirname "$REMOTE_WORKSPACE_FILE")
                REMOTE_SANDBOX_DIR=$(dirname "$REMOTE_SANDBOX_FILE")

                # Step 1: Update Tier 1 Public Workspace (Xed Visible)
                $ADB shell "mkdir -p $REMOTE_WORKSPACE_DIR"
                $ADB push "$current_file" "$REMOTE_WORKSPACE_FILE" > /dev/null

                # Step 2: Update Tier 2 Private App Sandbox via internal copy
                $ADB shell "run-as $APP_PACKAGE mkdir -p $REMOTE_SANDBOX_DIR"
                $ADB shell "run-as $APP_PACKAGE sh -c 'cp $REMOTE_WORKSPACE_FILE $REMOTE_SANDBOX_FILE'"

                # Step 3: Dispatch immediate WebView hard refresh signal
                echo "Broadcasting ACTION_RELOAD_WEBVIEW..."
                $ADB shell am broadcast -a "$APP_PACKAGE.ACTION_RELOAD_WEBVIEW" > /dev/null
                echo -e "${CLR_SUCCESS}-> Delta Sync Complete!${CLR_RESET}"
		beep
            fi
        fi
    done

    # ---------------------------------------------------------
    # INTERCEPT VECTOR B: NATIVE JAVA DELTA TRACKING
    # ---------------------------------------------------------
    for current_file in $(find "$JAVA_SCRATCH_DIR" -name "*.java"); do
        current_time=$(stat -c %Y "$current_file" 2>/dev/null || echo 0)
        last_time=${JAVA_FILE_TIMESTAMPS["$current_file"]}

        if [ -z "$last_time" ] || [ "$current_time" -gt "$last_time" ]; then
            JAVA_FILE_TIMESTAMPS["$current_file"]=$current_time
            
            if [ ! -z "$last_time" ]; then
                echo -e "\n${CLR_SUCCESS}[JAVA CHANGE DETECTED] Compiling: $(basename "$current_file")${CLR_RESET}"
                if [ -f "./scripts/inject_class.sh" ]; then
                    ./scripts/inject_class.sh "$current_file"
                else
                    echo -e "${CLR_WARN}Warning: scripts/inject_class.sh not found.${CLR_RESET}"
                fi
            fi
        fi
    done

    sleep 0.8
done

