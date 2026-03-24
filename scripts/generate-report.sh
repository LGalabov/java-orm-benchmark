#!/usr/bin/env bash
set -euo pipefail

RESULT_FILE="${1:-results/latest.json}"

if [ ! -f "$RESULT_FILE" ]; then
    echo "Error: File not found: $RESULT_FILE"
    echo "Usage: generate-report.sh [results-json-file]"
    exit 1
fi

echo "# Benchmark Results"
echo ""
echo "Generated from: $RESULT_FILE"
echo ""

# Stub: expand this as the results JSON schema is finalized
jq '.' "$RESULT_FILE"
