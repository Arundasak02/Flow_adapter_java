package com.flow.adapter.Model;

/**
 * Converts legacy GraphModel to UnifiedGraphModel format.
 * This handles the transformation from the old separated format
 * (methods, endpoints, topics, calls, etc.) to the new unified format
 * (nodes and edges).
 */
public class GraphModelConverter {

  /**
   * Convert legacy GraphModel to unified UnifiedGraphModel
   */
  public static UnifiedGraphModel convert(GraphModel legacy) {
    UnifiedGraphModel unified = new UnifiedGraphModel(legacy.projectId);

    // Step 1: Add all methods as nodes
    for (GraphModel.MethodNode method : legacy.methods.values()) {
      unified.addMethod(
        method.id,
        method.methodName,
        method.visibility,
        method.className,
        method.packageName,
        method.moduleName,
        method.signature
      );
    }

    // Step 2: Add all endpoints as nodes
    for (GraphModel.EndpointNode endpoint : legacy.endpoints.values()) {
      Node n = unified.ensureNode(endpoint.id, "ENDPOINT", endpoint.httpMethod + " " + endpoint.path);
      n.data.put("httpMethod", endpoint.httpMethod);
      n.data.put("path", endpoint.path);
      if (endpoint.produces != null && !endpoint.produces.isEmpty()) {
        n.data.put("produces", endpoint.produces);
      }
      if (endpoint.consumes != null && !endpoint.consumes.isEmpty()) {
        n.data.put("consumes", endpoint.consumes);
      }
    }

    // Step 3: Add all topics as nodes
    for (GraphModel.TopicNode topic : legacy.topics.values()) {
      Node n = unified.ensureNode(topic.id, "TOPIC", topic.name);
    }

    // Step 4: Add method call edges
    int callCounter = 0;
    for (GraphModel.CallEdge call : legacy.calls) {
      callCounter++;
      String edgeId = "e-call-" + callCounter;
      unified.addEdge(edgeId, call.from, call.to, "CALL");
    }

    // Step 5: Add endpoint edges (endpoint -> method)
    int endpointEdgeCounter = 0;
    for (GraphModel.EndpointEdge edge : legacy.endpointEdges) {
      endpointEdgeCounter++;
      String edgeId = "e-endpoint-" + endpointEdgeCounter;
      unified.addEdge(edgeId, edge.fromEndpoint, edge.toMethod, "HANDLES");
    }

    // Step 6: Add messaging edges
    int messagingCounter = 0;
    for (GraphModel.MessagingEdge edge : legacy.messaging) {
      messagingCounter++;
      String type = "produces".equals(edge.kind) ? "PRODUCES" : "CONSUMES";
      String edgeId = "e-" + edge.kind + "-" + messagingCounter;
      unified.addEdge(edgeId, edge.from, edge.to, type);
    }

    return unified;
  }
}

