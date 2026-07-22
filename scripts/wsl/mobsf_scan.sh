#!/bin/bash

# Configuration
API_KEY="97d46895c6b28692aa789e567f8d8f944d4494ee2854bb44673535120ff5a932"
HOST="http://localhost"
PORT="8000"
APK="./app/build/outputs/apk/debug/app-debug.apk"
BASE_URL="${HOST}:${PORT}"

# Verify the APK exists before running
if [ ! -f "$APK" ]; then
    echo "[DEBUG] ERROR: APK file not found at path: $APK"
    exit 1
fi

echo "========================================="
echo "Step 1: Uploading APK to MobSF..."
echo "========================================="
echo "[DEBUG] Sending file: $APK to ${BASE_URL}/api/v1/upload"

# Upload file and capture response
UPLOAD_RESP=$(curl -s -X POST \
  -F "file=@${APK}" \
  -H "Authorization: ${API_KEY}" \
  "${BASE_URL}/api/v1/upload")

echo "[DEBUG] Raw Upload Response: $UPLOAD_RESP"

# Robustly extract the hash value using sed regex pattern
HASH=$(echo "$UPLOAD_RESP" | sed -n 's/.*"hash"[[:space:]]*:[[:space:]]*"\([a-f0-9]*\)".*/\1/p')

if [ -z "$HASH" ]; then
    echo "Error: Failed to parse hash from MobSF response."
    exit 1
fi

echo "[DEBUG] Successfully parsed MD5 Hash: $HASH"
echo ""
echo "========================================="
echo "Step 2: Triggering Static Analysis Scan..."
echo "========================================="
echo "[DEBUG] Requesting scan for hash: $HASH"

# Trigger scan and wait for completion status
SCAN_RESP=$(curl -s -X POST \
  -d "scan_type=apk&hash=${HASH}" \
  -H "Authorization: ${API_KEY}" \
  "${BASE_URL}/api/v1/scan")

echo "[DEBUG] Raw Scan Trigger Response: $SCAN_RESP"
echo "Scan completed or initiated successfully."
echo ""
echo "========================================="
echo "Step 3: Downloading JSON Compliance Report..."
echo "========================================="
echo "[DEBUG] Requesting full JSON report payload for hash: $HASH"

# Fetch the JSON report file
OUTPUT_REPORT="mobsf-report.json"
curl -s -X POST \
  -d "hash=${HASH}" \
  -H "Authorization: ${API_KEY}" \
  "${BASE_URL}/api/v1/report_json" > "$OUTPUT_REPORT"

if [ -s "$OUTPUT_REPORT" ]; then
    echo "Success! Report saved locally to: $(pwd)/$OUTPUT_REPORT"
    echo "[DEBUG] Size of downloaded report: $(wc -c < "$OUTPUT_REPORT") bytes"
else
    echo "Error: Downloaded report is empty. Check MobSF container terminal logs."
    exit 1
fi
jq -c '{
  app_name: .app_name,
  package_name: .package_name,
  security_score: .security_score,
  target_sdk: .target_sdk,
  min_sdk: .min_sdk,
  vulnerabilities: {
    high: (if .appsec.high then [.appsec.high[] | {title: .title, description: .description}] else [] end),
    warning: (if .appsec.warning then [.appsec.warning[] | {title: .title, description: .description}] else [] end)
  },
  manifest_issues: (if .manifest_analysis.manifest_findings then [.manifest_analysis.manifest_findings[] | select(.severity == "high" or .severity == "warning") | {rule: .rule, severity: .severity, description: .description}] else [] end),
  network_issues: (if .network_security.network_findings then [.network_security.network_findings[] | select(.severity == "high" or .severity == "warning") | {scope: .scope, description: .description, severity: .severity}] else [] end)
}' mobsf-report.json > mobsf-min-report.json

jq -r '.vulnerabilities | to_entries[] | .key as $severity | .value[] | (if $severity == "high" then "\u001b[1;31m[HIGH]\u001b[0m" else "\u001b[1;33m[WARNING]\u001b[0m" end) as $tag | "\($tag) \u001b[1m\(.title)\u001b[0m\nDescription: \(.description)\n--------------------------------------------------"' mobsf-min-report.json

