#!/usr/bin/env bash
set -euo pipefail
DIR=$(cd "$(dirname "$0")" && pwd)
echo "Executing in directory: $DIR"
cd "$DIR"

echo "Building project (fast): full reactor package..."
mvn -q -DskipTests package || { echo "Initial build failed"; exit 1; }

SHADED="${DIR}/flow-runner/target/flow-runner-0.3.0-shaded.jar"
PLAIN="${DIR}/flow-runner/target/flow-runner-0.3.0.jar"

if [[ -f "$SHADED" ]]; then
  JAR="$SHADED"
  echo "Using shaded runner jar: $JAR"
else
  echo "Shaded jar not found. Building flow-runner module with shade..."
  mvn -q -DskipTests -pl flow-runner -am package || { echo "Failed to build flow-runner with shade"; exit 1; }
  if [[ -f "$SHADED" ]]; then
    JAR="$SHADED"
    echo "Built shaded jar: $JAR"
  elif [[ -f "$PLAIN" ]]; then
    JAR="$PLAIN"
    echo "Shaded jar still not found; falling back to plain jar: $JAR"
    echo "Note: the plain jar may not include dependencies; if runtime errors occur, build a shaded executable jar."
  else
    echo "No runnable jar found in flow-runner/target. Build failed." >&2
    exit 1
  fi
fi

echo "Running scanner using $JAR"
java -jar "$JAR" scan \
  --src sample/greens-order/src/main/java \
  --config sample/greens-order/src/main/resources \
  --out "$DIR/flow.json" \
  --project payment-service-project

echo "Scanner finished."
echo "Output: $DIR/flow.json"
