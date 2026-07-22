#!/bin/bash
# Manage Android runtime permissions for the app via adb.
# All permission lists are read from the running package on the device,
# so this script does not depend on project source files.
# Usage: permissions.sh [available|list|add <permission>|remove <permission>|add-all|remove-all]
source "$(dirname "$0")/common.sh"
adb_check

usage() {
    cat <<EOF
Usage: $(basename "$0") <command> [args]

Commands:
  available              List permissions requested by the installed package.
  list                   List current permission grant state for the package.
  add <permission>       Grant a single permission (e.g., android.permission.CAMERA).
  remove <permission>    Revoke a single permission.
  add-all                Grant all runtime permissions requested by the package.
  remove-all             Revoke all currently granted runtime permissions.

Examples:
  $(basename "$0") available
  $(basename "$0") add android.permission.CAMERA
  $(basename "$0") remove android.permission.RECORD_AUDIO
EOF
}

# Extract the "requested permissions" block from pm dump.
requested_permissions() {
    adb shell pm dump "$PACKAGE" 2>/dev/null | awk '
        /^[[:space:]]*requested permissions:[[:space:]]*$/ { in_block=1; next }
        /^[[:space:]]*install permissions:[[:space:]]*$/ { in_block=0 }
        /^[[:space:]]*User [0-9]+:/ { in_block=0 }
        in_block && /^[[:space:]]*android\.permission\./ { print $1 }
    ' | sed 's/^[[:space:]]*//' | sort -u
}

granted_permissions() {
    adb shell pm dump "$PACKAGE" 2>/dev/null | grep -E "^\s*android\.permission\.\S+:\s*granted=true" | awk -F: '{print $1}' | sed 's/^[[:space:]]*//' | sort -u
}

cmd_available() {
    echo "Permissions requested by $PACKAGE on device:"
    requested_permissions | sed 's/^/  /'
}

cmd_list() {
    echo "Current permission state for $PACKAGE:"
    adb shell pm dump "$PACKAGE" 2>/dev/null | grep -E "^\s*android\.permission\.\S+:\s*granted=" | sed 's/^/  /'
}

cmd_add() {
    local perm="$1"
    if [ -z "$perm" ]; then
        echo "Error: permission name required." >&2
        usage
        exit 1
    fi
    echo "Granting $perm..."
    adb shell pm grant "$PACKAGE" "$perm" || echo "Warning: could not grant $perm (may be non-runtime or restricted)." >&2
}

cmd_remove() {
    local perm="$1"
    if [ -z "$perm" ]; then
        echo "Error: permission name required." >&2
        usage
        exit 1
    fi
    echo "Revoking $perm..."
    adb shell pm revoke "$PACKAGE" "$perm" || echo "Warning: could not revoke $perm." >&2
}

cmd_add_all() {
    echo "Granting all requested runtime permissions..."
    mapfile -t perms < <(requested_permissions)
    for perm in "${perms[@]}"; do
        [ -n "$perm" ] || continue
        if adb shell pm grant "$PACKAGE" "$perm" >/dev/null 2>&1; then
            echo "  granted: $perm"
        else
            echo "  skipped: $perm"
        fi
    done
}

cmd_remove_all() {
    echo "Revoking all granted runtime permissions..."
    mapfile -t perms < <(granted_permissions)
    for perm in "${perms[@]}"; do
        [ -n "$perm" ] || continue
        if adb shell pm revoke "$PACKAGE" "$perm" >/dev/null 2>&1; then
            echo "  revoked: $perm"
        else
            echo "  failed:  $perm"
        fi
    done
}

COMMAND="${1:-}"
case "$COMMAND" in
    available)
        cmd_available
        ;;
    list)
        cmd_list
        ;;
    add)
        cmd_add "$2"
        ;;
    remove)
        cmd_remove "$2"
        ;;
    add-all)
        cmd_add_all
        ;;
    remove-all)
        cmd_remove_all
        ;;
    -h|--help|help|""|*)
        usage
        exit 1
        ;;
esac
