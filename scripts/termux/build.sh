#!/bin/bash
set -e

export JAVA_HOME='/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk'
export ANDROID_HOME='/data/data/com.termux/files/home/android-sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

cd "$(dirname "$0")/.."

echo "Building APK..."
# Pass the AAPT2 override dynamically for Termux environments
./gradlew assembleDebug assembleRelease \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
  --no-daemon

echo ""
echo "APKs built:"
ls -lh app/build/outputs/apk/debug/*.apk 2>/dev/null || true
ls -lh app/build/outputs/apk/release/*.apk 2>/dev/null || true

