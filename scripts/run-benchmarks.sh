#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

QUICK=false
DB="all"
SKIP_BUILD=false

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --quick       Reduce JMH iterations for faster feedback"
    echo "  --db <name>   Database to benchmark: postgres, mysql, or all (default: all)"
    echo "  --skip-build  Skip Gradle build (reuse existing JAR)"
    echo "  -h, --help    Show this help"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --quick)     QUICK=true; shift ;;
        --db)        DB="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=true; shift ;;
        -h|--help)   usage ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

cd "$PROJECT_DIR"

echo "==> Starting databases..."
docker compose -f docker/docker-compose.yml up -d

echo "==> Waiting for databases to be ready..."
"$SCRIPT_DIR/wait-for-db.sh" postgresql localhost 5432
"$SCRIPT_DIR/wait-for-db.sh" mysql localhost 3306

if [ "$SKIP_BUILD" = false ]; then
    echo "==> Building project..."
    ./gradlew clean shadowJar
fi

echo "==> Running benchmarks (quick=$QUICK, db=$DB)..."
QUICK_MODE="$QUICK" DB_TARGET="$DB" \
    java -jar benchmark-harness/build/libs/benchmarks.jar

echo "==> Stopping databases..."
docker compose -f docker/docker-compose.yml down

echo "==> Done. Results in results/latest.json"
