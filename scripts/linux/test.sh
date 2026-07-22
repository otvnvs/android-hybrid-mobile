#!/bin/bash
set -e

# Dynamically locate the directory where this script resides to maintain portability
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INVOKE_PY="$SCRIPT_DIR/test-invoke.py"

# Define ANSI colours for cleaner reporting output diagnostics
GREEN='' #'\033[0;32m'
BLUE='' #'\033[0;34m'
YELLOW='' #'\033[1;33m'
RED='' # '\033[0;31m'
NC='' # '\033[0;3m' # No Colour

echo -e "${BLUE}===================================================================${NC}"
echo -e "${BLUE}     AHM PORTABLE HYBRID FRAMEWORK INTERFACE AUTOMATED TEST SUITE  ${NC}"
echo -e "${BLUE}===================================================================${NC}"

# Verify the Python invocation script can be resolved before processing loops
if [ ! -f "$INVOKE_PY" ]; then
    echo -e "${RED}Error: Cannot find test-invoke.py execution script at: $INVOKE_PY${NC}"
    exit 1
fi

# Helper function to print structural test group headers
print_header() {
    echo -e "\n${YELLOW}🧪 [TEST GROUP] $1...${NC}"
    echo "-------------------------------------------------------------------"
}

# Helper function to format and pipe JSON execution commands over to test-invoke.py
run_test() {
    local description="$1"
    local json_payload="$2"
    
    echo -e "${BLUE}👉 Running: $description...${NC}"
    # Fire the query using the established Python tool engine and handle result output
    if python3 "$INVOKE_PY" "$json_payload"; then
        echo -e "${GREEN}✅ PASSED${NC}\n"
    else
        echo -e "${RED}❌ FAILED${NC}\n"
    fi
}

# =====================================================================
# GROUP 1: EXAMPLE CONTROLLER MUTATIONS (POST & GET PARAMETERS)
# =====================================================================
print_header "ExampleController Operations"

run_test "GET Request with query tracking fields extraction" \
  '{"type":"invoke","controller":"ExampleController","method":"testGetMethod","path":"/api/example/get-test?tracking_id=wsl_automated_run&filter=bash_suite","http_method":"GET"}'

run_test "POST Mutation with input payload parameters injection" \
  '{"type":"invoke","controller":"ExampleController","method":"testPostMethod","path":"/api/example/mutation-test","http_method":"POST","requested_status_code":201,"message_payload":"Hello from automated bash script!"}'


# =====================================================================
# GROUP 2: SQLITE DATABASE LAYER CONTROLLER (DATABASE MANAGEMENT)
# =====================================================================
print_header "DatabaseController SQLite Lifecycle Persistence"

DB_TARGET_PATH="/data/data/com.example.app/files/automated_suite.db"

run_test "Initialize a completely new blank SQLite database file container" \
  "{\"type\":\"invoke\",\"controller\":\"DatabaseController\",\"method\":\"createDatabaseFile\",\"path\":\"/api/database/create\",\"http_method\":\"POST\",\"payload\":{\"path\":\"$DB_TARGET_PATH\"}}"

run_test "Compile and execute a parameterized row insertion statement schema mutation" \
  "{\"type\":\"invoke\",\"controller\":\"DatabaseController\",\"method\":\"executeDatabaseStatement\",\"path\":\"/api/database/execute\",\"http_method\":\"POST\",\"payload\":{\"path\":\"$DB_TARGET_PATH\",\"sql\":\"CREATE TABLE IF NOT EXISTS system_faults (id INTEGER PRIMARY KEY, error_tag TEXT);\"}}"


# =====================================================================
# GROUP 3: PERSISTENT LOCAL FILE SYSTEM STORAGE (FS OPERATIONS)
# =====================================================================
print_header "FsController Native Partition Operations"

run_test "Extract absolute hardware environmental system location path mapping variables" \
  '{"type":"invoke","controller":"FsController","method":"getStorageLocations","path":"/api/fs/locations","http_method":"GET"}'

run_test "Query total internal disk storage space block metrics diagnostics data profile" \
  '{"type":"invoke","controller":"FsController","method":"getDiskSpaceDiagnostics","path":"/api/fs/diskspace","http_method":"GET"}'

echo -e "${BLUE}===================================================================${NC}"
echo -e "${GREEN}🎉 Test Execution Suite Sequence Run Finished Complete.${NC}"
echo -e "${BLUE}===================================================================${NC}"

