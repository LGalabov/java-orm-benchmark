#!/usr/bin/env bash
set -euo pipefail

DB_TYPE="${1:?Usage: wait-for-db.sh <postgresql> <host> <port>}"
HOST="${2:-localhost}"
PORT="${3:-5432}"
MAX_RETRIES="${4:-30}"
RETRY_INTERVAL=2

echo "Waiting for $DB_TYPE at $HOST:$PORT..."

for i in $(seq 1 "$MAX_RETRIES"); do
    if nc -z "$HOST" "$PORT" 2>/dev/null; then
        echo "$DB_TYPE is ready (attempt $i/$MAX_RETRIES)"
        exit 0
    fi
    echo "  Attempt $i/$MAX_RETRIES — not ready, waiting ${RETRY_INTERVAL}s..."
    sleep "$RETRY_INTERVAL"
done

echo "Error: $DB_TYPE at $HOST:$PORT did not become ready after $MAX_RETRIES attempts"
exit 1
