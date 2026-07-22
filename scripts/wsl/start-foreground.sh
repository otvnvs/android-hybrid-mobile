# Define your target paths
ADB="/mnt/c/usr/bin/adb.exe"
PACKAGE="com.example.app"

# Start the application normally into the foreground with custom test arguments
$ADB shell am start \
    -n "$PACKAGE/$PACKAGE.MainActivity" \
    --es "test_host" "127.0.0.1" \
    --ei "test_port" 9999
