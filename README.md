# Flow Java Adapter

A powerful multi-module Java application that scans Spring/Kafka projects and extracts an architectural graph (methods, endpoints, topics, call relationships) into a JSON file for visualization and analysis.

## Quick Start

```bash
# Build and run the example scanner
./run-example.sh
```

Output: `payment-service-graph.json`

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           FLOW JAVA ADAPTER - RUNTIME FLOW                         │
└─────────────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────────┐
                              │  flow-runner.jar    │
                              │  (executable jar)   │
                              └──────────┬──────────┘
                                         │
                                         │ java -jar
                                         ▼
                    ┌────────────────────────────────────────┐
                    │         ScanCommand                    │
                    │  (parses CLI args, starts scan)        │
                    └────────────┬───────────────────────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
                ▼                ▼                ▼
      ┌──────────────────┐  ┌──────────────┐  ┌─────────────────────┐
      │  ConfigLoader    │  │  GraphModel  │  │  ServiceLoader      │
      │  (load .props)   │  │  (aggregate) │  │  (load plugins)     │
      └──────────────────┘  └──────────────┘  └─────────────────────┘
                                 ▲                        │
                                 │                        │
              ┌──────────────────┤                        ▼
              │                  │         ┌──────────────────────────────┐
              │                  │         │   JavaSourceScanner          │
              │                  │         │   (walk source tree)         │
              │                  │         └──────────────────────────────┘
              │                  │                        │
              │      ┌───────────┼───────────┐            │
              │      │           │           │            │
              │      ▼           ▼           ▼            ▼
        ┌──────────────────┐ ┌─────────────────────┐ ┌──────────────────────┐
        │ SpringEndpoint   │ │  KafkaScanner       │ │ MethodCallAnalyzer   │
        │ Scanner          │ │  (@KafkaListener)   │ │ (analyze method body)│
        │ (Spring annot.)  │ │  (@Input/@Output)   │ │                      │
        └────────┬─────────┘ └──────────┬──────────┘ └──────────┬───────────┘
                 │                      │                       │
        Extracts:│                      │                       │Produces:
        ┌────────▼──────────┐  ┌────────▼──────────┐   ┌───────▼─────────┐
        │ - EndpointNodes   │  │ - TopicNodes      │   │ - CallEdges     │
        │ - path            │  │ - topic names     │   │ (method->method)│
        │ - httpMethod      │  │ - MessagingEdges  │   └─────────────────┘
        │ - produces[]      │  │ (consumes/produce)│
        │ - consumes[]      │  └───────┬───────────┘
        └────────┬──────────┘          │
                 │                     │
                 └─────────┬───────────┘
                           │
                           ▼
                    ┌──────────────────┐
                    │  GraphModel      │
                    │  (aggregate all: │
                    │   - methods      │
                    │   - endpoints    │
                    │   - topics       │
                    │   - calls        │
                    │   - messaging)   │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────────┐
                    │ GraphExporterJson    │
                    │ (serialize + write)  │
                    └────────┬─────────────┘
                             │
                             ▼
                    ┌──────────────────────┐
                    │payment-service-      │
                    │graph.json            │
                    │(output artifact)     │
                    └──────────────────────┘
```

## Architecture Overview

### Core Components

- **flow-runner** — Executable shaded JAR that runs the scanner
- **flow-adapter** — Core scanning framework and GraphModel
- **flow-spring-plugin** — Scans Spring mapping annotations (@GetMapping, @PostMapping, @RequestMapping)
- **flow-kafka-plugin** — Scans Kafka listener/producer annotations (@KafkaListener, @Input, @Output)

### Key Classes

| Class | Role |
|-------|------|
| `ScanCommand` | Entry point; orchestrates scanning |
| `GraphModel` | Data model (methods, endpoints, topics, edges) |
| `JavaSourceScanner` | Walks source tree and delegates to plugins |
| `SpringEndpointScanner` | Extracts HTTP endpoints with produces/consumes |
| `KafkaScanner` | Extracts Kafka topics and messaging edges |
| `MethodCallAnalyzer` | Analyzes method bodies to find calls |
| `GraphExporterJson` | Writes GraphModel to JSON using Jackson |

### Output (payment-service-graph.json)

```json
{
  "projectId": "...",
  "schema": "gef:1.1",
  "methods": { "com.example.Service#method(...)": {...} },
  "endpoints": { "endpoint:GET /path": {...} },
  "topics": { "topic:kafka.topic": {...} },
  "calls": [ {"from": "...", "to": "...", "kind": "calls"} ],
  "endpointEdges": [ {"fromEndpoint": "...", "toMethod": "..."} ],
  "messaging": [ {"from": "...", "to": "...", "kind": "produces|consumes"} ]
}
```

## Building and Running

### Build
```bash
mvn -T 1C -DskipTests clean package
```

### Run Scanner
```bash
./run-example.sh
```

or manually:
```bash
java -jar flow-runner/target/flow-runner-0.3.0.jar scan \
  --src <source-dir> \
  --config <config-dir> \
  --out <output.json> \
  --project <project-id>
```

### Dependency Analysis
```bash
mvn -DskipTests dependency:analyze
```

## Project Structure

```
.
├── flow-adapter/               # Core scanning framework
│   ├── src/main/java/
│   │   ├── Model/GraphModel.java
│   │   ├── GraphExporterJson.java
│   │   ├── ScanCommand.java
│   │   ├── util/
│   │   │   ├── ConfigLoader.java
│   │   │   └── SignatureUtil.java
│   │   └── scanners/
│   │       ├── JavaSourceScanner.java
│   │       └── MethodCallAnalyzer.java
│   └── pom.xml
├── flow-spring-plugin/         # Spring endpoint scanner plugin
│   ├── src/main/java/
│   │   └── com/flow/plugin/spring/SpringEndpointScanner.java
│   └── src/main/resources/META-INF/services/
│       └── com.flow.adapter.FlowPlugin
├── flow-kafka-plugin/          # Kafka topic scanner plugin
│   ├── src/main/java/
│   │   └── com/flow/plugin/kafka/KafkaScanner.java
│   └── src/main/resources/META-INF/services/
│       └── com.flow.adapter.FlowPlugin
├── flow-runner/                # Executable runner (shaded jar)
│   └── pom.xml
├── sample/greens-order/        # Example Spring/Kafka project
├── docs/                       # Documentation
│   ├── flow-diagram.puml       # PlantUML source
│   └── flow-diagram.svg        # SVG version
├── pom.xml                     # Parent POM
├── .gitignore                  # Excludes build artifacts
└── payment-service-graph.json  # Scanner output
```

## Key Features

- ✅ **Multi-module design** — plugins register via ServiceLoader (META-INF/services)
- ✅ **Spring integration** — scans @GetMapping, @PostMapping, @RequestMapping
- ✅ **Kafka integration** — scans @KafkaListener, @Input, @Output
- ✅ **Method analysis** — extracts call graph and method signatures
- ✅ **Production-ready code** — SLF4J logging, no deprecated APIs, resource leaks fixed
- ✅ **Clean dependencies** — minimal bloat, only required libs per module
- ✅ **Shaded runner** — single executable jar with all dependencies included

## Important Files to Keep in Version Control

- `flow-spring-plugin/src/main/resources/META-INF/services/com.flow.adapter.FlowPlugin` — plugin service provider
- `flow-kafka-plugin/src/main/resources/META-INF/services/com.flow.adapter.FlowPlugin` — plugin service provider
- `.gitignore` — ensures build artifacts are not committed

## Example Command

```bash
java -jar flow-runner/target/flow-runner-0.3.0.jar scan \
  --src sample/greens-order/src/main/java \
  --config sample/greens-order/src/main/resources \
  --out payment-service-graph.json \
  --project payment-service-project
```

## Notes

- The scanner produces a clean JSON graph suitable for architecture visualization tools.
- Each module declares only its required dependencies (no transitive bloat).
- Plugins are loaded at runtime via ServiceLoader — add new scanners by implementing FlowPlugin and registering in META-INF/services.
- The graph includes endpoint metadata (produces/consumes as lists) and message flow relationships (consumes/produces kinds).

