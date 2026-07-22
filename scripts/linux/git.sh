#!/bin/bash

# Exit if any individual git command fails unexpectedly
set -e

# 1. Automatically detect the primary branch (handles main, master, or a feature branch)
TARGET_BRANCH=$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's@^origin/@@')
: ${TARGET_BRANCH:=main}

# Find the very first commit in the repository history
FIRST_COMMIT=$(git rev-list --max-parents=0 HEAD | tail -n 1)
# Find the latest commit on the target branch
LATEST_COMMIT=$(git rev-parse "$TARGET_BRANCH")
# Find where we are currently standing
CURRENT_COMMIT=$(git rev-parse HEAD)

# 2. Process the flags
case "$1" in
    --backward)
        if [ "$CURRENT_COMMIT" = "$FIRST_COMMIT" ]; then
            echo "❌ Error: Cannot move backward. You are already at the BEGINNING of the project history." >&2
            exit 1
        fi
        echo "⏮️  Stepping BACKWARD one commit..."
        git checkout HEAD~1 >/dev/null 2>&1
        ;;
        
    --forward)
        if [ "$CURRENT_COMMIT" = "$LATEST_COMMIT" ]; then
            echo "❌ Error: Cannot move forward. You are already at the END ($TARGET_BRANCH) of the project history." >&2
            exit 1
        fi
        
        echo "⏭️  Stepping FORWARD one commit toward $TARGET_BRANCH..."
        NEXT_COMMIT=$(git rev-list --topo-order HEAD.."$TARGET_BRANCH" | tail -1)
        
        if [ -z "$NEXT_COMMIT" ]; then
            echo "❌ Error: No forward commits found. You are already at the latest commit on $TARGET_BRANCH!" >&2
            exit 1
        fi
        git checkout "$NEXT_COMMIT" >/dev/null 2>&1
        ;;
        
    --begin)
        if [ "$CURRENT_COMMIT" = "$FIRST_COMMIT" ]; then
            echo "⚠️  You are already at the beginning of the project."
        else
            echo "⏳ Jumping to the BEGINNING of the project (Root/First Commit)..."
            git checkout "$FIRST_COMMIT" >/dev/null 2>&1
        fi
        ;;
        
    --end)
        if [ "$CURRENT_COMMIT" = "$LATEST_COMMIT" ]; then
            echo "⚠️  You are already at the end ($TARGET_BRANCH) of the project."
        else
            echo "🚀 Jumping to the END (Latest commit on $TARGET_BRANCH)..."
            git checkout "$TARGET_BRANCH" >/dev/null 2>&1
        fi
        ;;
        
    *)
        echo "Usage: $0 [--forward | --backward | --begin | --end]"
        exit 1
        ;;
esac

# 3. Calculate and print the progress indicator (COMMIT X OF Y)
# Total number of commits on the target branch timeline
TOTAL_COMMITS=$(git rev-list --count "$TARGET_BRANCH")
# Current commit index (how many commits from the root up to where we are standing now)
CURRENT_INDEX=$(git rev-list --count HEAD)

echo "========================================"
echo "📍 STATUS: COMMIT $CURRENT_INDEX OF $TOTAL_COMMITS"
echo "========================================"
git log -n 1 --oneline
echo ""

# 4. Automatically run your build pipeline after successful navigation
echo "🔄 Running build pipeline..."
./scripts/build.sh && ./scripts/install.sh && ./scripts/restart.sh

