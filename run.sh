#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# Ensure required commands exist
for cmd in mvn java; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: required command '$cmd' not found in PATH"
    exit 1
  fi
done

echo "Building project (skip tests)..."
mvn -DskipTests package

echo
echo "Available JARs in target directories (top results):"
find . -name "*.jar" -path "*/target/*" | head -20 || true

# Prefer shaded jar if present, otherwise take the newest flow-runner jar
JAR=$(find flow-runner/target -maxdepth 1 -type f -name 'flow-runner-*-shaded.jar' -print0 | xargs -0 ls -1t 2>/dev/null | head -n1 || true)
if [ -z "$JAR" ]; then
  JAR=$(find flow-runner/target -maxdepth 1 -type f -name 'flow-runner-*.jar' -print0 | xargs -0 ls -1t 2>/dev/null | head -n1 || true)
fi

if [ -z "$JAR" ]; then
  echo "Error: no flow-runner JAR found in flow-runner/target"
  echo "Contents of flow-runner/target:"
  ls -la flow-runner/target || true
  exit 1
fi

echo
echo "Using JAR: $JAR"

SRC_DIR="sample/greens-order/src/main/java"
CONFIG_DIR="sample/greens-order/src/main/resources"

if [ ! -d "$SRC_DIR" ]; then
  echo "Error: source directory $SRC_DIR not found"
  exit 1
fi

if [ ! -d "$CONFIG_DIR" ]; then
  echo "Error: config directory $CONFIG_DIR not found"
  exit 1
fi

OUT_FILE="flow.json"
PROJECT_NAME="payment-service-project"

echo
echo "Running scanner..."
echo "java -jar '$JAR' scan --src '$SRC_DIR' --config '$CONFIG_DIR' --out '$OUT_FILE' --project '$PROJECT_NAME'"

java -jar "$JAR" scan \
  --src "$SRC_DIR" \
  --config "$CONFIG_DIR" \
  --out "$OUT_FILE" \
  --project "$PROJECT_NAME"

echo "Done: $OUT_FILE"
