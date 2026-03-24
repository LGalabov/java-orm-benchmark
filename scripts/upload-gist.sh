#!/usr/bin/env bash
set -euo pipefail

RESULT_FILE="${1:?Usage: upload-gist.sh <results-json-file>}"
GIST_ID="${GIST_ID:?Set GIST_ID env var}"
GIST_TOKEN="${GIST_TOKEN:?Set GIST_TOKEN env var}"

if [ ! -f "$RESULT_FILE" ]; then
    echo "Error: File not found: $RESULT_FILE"
    exit 1
fi

TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

PAYLOAD=$(jq -n \
    --arg desc "Java ORM Benchmark results — ${TIMESTAMP}" \
    --arg content "$(cat "$RESULT_FILE")" \
    '{
        "description": $desc,
        "files": {
            "benchmark-results.json": { "content": $content }
        }
    }')

RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH \
    -H "Authorization: token ${GIST_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/gists/${GIST_ID}" \
    -d "$PAYLOAD")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo "Results uploaded to https://gist.github.com/${GIST_ID}"
else
    echo "Error uploading to Gist (HTTP $HTTP_CODE):"
    echo "$BODY" | jq -r '.message // .' 2>/dev/null || echo "$BODY"
    exit 1
fi
