#!/usr/bin/env bash
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
cd "$DIR"
mvn -q -DskipTests package
java -jar target/flow-java-adapter-0.3.0-shaded.jar scan --src sample/greens-order/src/main/java --config sample/greens-order/src/main/resources --out sample/greens-order/graph.json --project greens-order-system
echo "Sample graph written to sample/greens-order/graph.json"
