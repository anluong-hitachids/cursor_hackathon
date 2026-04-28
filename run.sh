#!/usr/bin/env bash
# Self-contained ETL demo runner.  Requires JDK 17+.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
    JAVAC_EXE="$JAVA_HOME/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVA_EXE="java"
    JAVAC_EXE="javac"
else
    echo "[ERROR] No JDK found.  Install JDK 17+ (https://adoptium.net/) and either" >&2
    echo "        put it on PATH or set JAVA_HOME." >&2
    exit 1
fi

mkdir -p bin

SOURCES=$(mktemp)
trap 'rm -f "$SOURCES"' EXIT
find src -name "*.java" >"$SOURCES"

echo "Compiling sources..."
"$JAVAC_EXE" -d bin @"$SOURCES"

echo
echo "Using Java:"
"$JAVA_EXE" -version
echo

"$JAVA_EXE" -cp bin etl.EtlPipeline
