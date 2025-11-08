#!/usr/bin/env bash
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
echo "Executing in directory: $DIR"
cd "$DIR"

echo "Building project..."
mvn -DskipTests package

echo "Build complete. Running scanner..."
java -jar target/flow-parent-0.3.0-shaded.jar scan \
  --src sample/greens-order/src/main/java \
  --config sample/greens-order/src/main/resources \
  --out "$DIR/payment-service-graph.json" \
  --project payment-service-project

echo "Scanner finished."
echo "Output: $DIR/payment-service-graph.json"
