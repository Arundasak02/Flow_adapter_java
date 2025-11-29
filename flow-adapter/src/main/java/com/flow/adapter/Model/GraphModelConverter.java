package com.flow.adapter.Model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converts legacy GraphModel to UnifiedGraphModel format.
 * This handles the transformation from the old separated format
 * (methods, endpoints, topics, calls, etc.) to the new unified format
 * (nodes and edges).
 *
 * Key normalizations:
 * 1. Normalizes method signatures (removes fully-qualified type names)
 * 2. Deduplicates method nodes with same normalized ID
 * 3. Adds CLASS and SERVICE nodes for better graph structure
 * 4. Fixes CONSUMES edge direction (from topic to method)
 * 5. Normalizes endpoint IDs to strict format
 */
public class GraphModelConverter {

  private static final String CALL_EDGE_TYPE = "CALL";
  private static final String HANDLES_EDGE_TYPE = "HANDLES";
  private static final String PRODUCES_EDGE_TYPE = "PRODUCES";
  private static final String CONSUMES_EDGE_TYPE = "CONSUMES";

  /**
   * Convert legacy GraphModel to unified UnifiedGraphModel
   */
  public static UnifiedGraphModel convert(GraphModel legacy) {
    UnifiedGraphModel unified = new UnifiedGraphModel(legacy.projectId);

    // Track which normalized method IDs we've already processed (for deduplication)
    Set<String> processedMethodIds = new HashSet<>();
    Map<String, GraphModel.MethodNode> normalizedMethods = new HashMap<>();

    // Step 1: Normalize and deduplicate method nodes
    for (GraphModel.MethodNode method : legacy.methods.values()) {
      String normalizedId = SignatureNormalizer.createNormalizedMethodId(
        method.className,
        method.methodName,
        method.signature
      );

      // Only process each normalized ID once (skip duplicates)
      if (!processedMethodIds.contains(normalizedId)) {
        normalizedMethods.put(normalizedId, method);
        processedMethodIds.add(normalizedId);
      }
    }

    // Step 2: Add all normalized methods as nodes
    Map<String, String> classToServiceMap = new HashMap<>();
    for (Map.Entry<String, GraphModel.MethodNode> entry : normalizedMethods.entrySet()) {
      String normalizedId = entry.getKey();
      GraphModel.MethodNode method = entry.getValue();

      String normalizedSig = method.signature != null ?
        SignatureNormalizer.normalizeSignature(method.signature) : "";

      // Fix legacy data: extract simple className and correct packageName
      String className = method.className;
      String packageName = method.packageName;

      // Case 1: className is FQN like "com.greens.order.core.OrderService"
      if (className != null && className.contains(".") && !className.startsWith("com.")) {
        // className contains FQN, packageName is just the package
        int lastDot = className.lastIndexOf('.');
        packageName = className.substring(0, lastDot);
        className = className.substring(lastDot + 1);
      }
      // Case 2: Both contain FQN (className="com.greens.order.core.OrderService", packageName="com.greens.order.core.OrderService")
      else if (className != null && packageName != null && className.contains(".")) {
        // Extract just the simple name from className
        int lastDot = className.lastIndexOf('.');
        packageName = className.substring(0, lastDot);
        className = className.substring(lastDot + 1);
      }

      unified.addMethod(
        normalizedId,
        method.methodName,
        method.visibility,
        className,
        packageName,
        method.moduleName,
        normalizedSig
      );

      // Track class-to-service mapping for later
      if (className != null && packageName != null) {
        String classId = packageName + "." + className;
        String serviceName = SignatureNormalizer.deriveServiceName(method.moduleName, packageName);
        classToServiceMap.put(classId, serviceName);
      }
    }

    // Step 3: Add all endpoints as nodes (normalized IDs)
    for (GraphModel.EndpointNode endpoint : legacy.endpoints.values()) {
      String normalizedEndpointId = SignatureNormalizer.normalizeEndpointId(endpoint.httpMethod, endpoint.path);
      Node n = unified.ensureNode(normalizedEndpointId, "ENDPOINT", endpoint.httpMethod + " " + endpoint.path);
      n.data.put("httpMethod", endpoint.httpMethod);
      n.data.put("path", endpoint.path);
      if (endpoint.produces != null && !endpoint.produces.isEmpty()) {
        n.data.put("produces", endpoint.produces);
      }
      if (endpoint.consumes != null && !endpoint.consumes.isEmpty()) {
        n.data.put("consumes", endpoint.consumes);
      }
    }

    // Step 4: Add all topics as nodes (normalized IDs)
    for (GraphModel.TopicNode topic : legacy.topics.values()) {
      String normalizedTopicId = SignatureNormalizer.normalizeTopicId(topic.id);
      Node n = unified.ensureNode(normalizedTopicId, "TOPIC", topic.name);
    }

    // Step 5: Add CLASS nodes and CLASS->SERVICE edges
    for (Map.Entry<String, String> classEntry : classToServiceMap.entrySet()) {
      String classId = classEntry.getKey();  // e.g., "com.greens.order.core.OrderService"
      String serviceName = classEntry.getValue();

      // Extract simple className from classId (after last dot only)
      int lastDot = classId.lastIndexOf('.');
      String packageName = lastDot > 0 ? classId.substring(0, lastDot) : "";
      String className = lastDot > 0 ? classId.substring(lastDot + 1) : classId;

      // Create CLASS node directly with correct classId (do not rebuild it)
      Node classNode = unified.ensureNode(classId, "CLASS", className);
      classNode.data.put("className", className);  // Just simple name, not FQN
      classNode.data.put("packageName", packageName);  // Just the package, not duplicated
      classNode.data.put("moduleName", serviceName);

      // Add SERVICE node
      String serviceId = "service:" + serviceName;
      unified.addService(serviceName, serviceName);

      // Add CLASS->SERVICE edge
      unified.addClassToServiceEdge(classId, serviceId);
    }

    // Step 6: Add method call edges (normalized IDs)
    int callCounter = 0;
    for (GraphModel.CallEdge call : legacy.calls) {
      callCounter++;
      String normalizedFrom = normalizeMethodIdInEdge(call.from);
      String normalizedTo = normalizeMethodIdInEdge(call.to);
      String edgeId = "e-call-" + callCounter;
      unified.addEdge(edgeId, normalizedFrom, normalizedTo, CALL_EDGE_TYPE);
    }

    // Step 7: Add endpoint edges (endpoint -> method with normalized IDs)
    int endpointEdgeCounter = 0;
    for (GraphModel.EndpointEdge edge : legacy.endpointEdges) {
      endpointEdgeCounter++;
      String normalizedEndpointId = SignatureNormalizer.normalizeEndpointId(
        // Extract httpMethod and path from endpoint ID
        extractHttpMethodFromEndpointId(edge.fromEndpoint),
        extractPathFromEndpointId(edge.fromEndpoint)
      );
      String normalizedMethodId = normalizeMethodIdInEdge(edge.toMethod);
      String edgeId = "e-endpoint-" + endpointEdgeCounter;
      unified.addEdge(edgeId, normalizedEndpointId, normalizedMethodId, HANDLES_EDGE_TYPE);
    }

    // Step 8: Add messaging edges (with corrected CONSUMES direction)
    int messagingCounter = 0;
    for (GraphModel.MessagingEdge edge : legacy.messaging) {
      messagingCounter++;
      String edgeId = "e-" + edge.kind + "-" + messagingCounter;

      if ("produces".equals(edge.kind)) {
        // PRODUCES: method -> topic
        String normalizedFrom = normalizeMethodIdInEdge(edge.from);
        String normalizedTo = SignatureNormalizer.normalizeTopicId(edge.to);
        unified.addEdge(edgeId, normalizedFrom, normalizedTo, PRODUCES_EDGE_TYPE);
      } else if ("consumes".equals(edge.kind)) {
        // CONSUMES: topic -> method
        // Note: edge.from may contain method ID, edge.to contains topic
        // We need to swap: topic should be FROM, method should be TO
        String topicId = extractTopicNameFromMessagingEdge(edge.to);
        String methodId = normalizeMethodIdInEdge(edge.from);
        String normalizedTopic = SignatureNormalizer.normalizeTopicId(topicId);
        unified.addEdge(edgeId, normalizedTopic, methodId, CONSUMES_EDGE_TYPE);
      }
    }

    // Step 9: Add METHOD->CLASS edges
    for (Map.Entry<String, GraphModel.MethodNode> entry : normalizedMethods.entrySet()) {
      String methodId = entry.getKey();
      GraphModel.MethodNode method = entry.getValue();

      // Use same normalization as Step 2
      String className = method.className;
      String packageName = method.packageName;

      // Fix legacy data: extract simple className and correct packageName
      if (className != null && className.contains(".")) {
        int lastDot = className.lastIndexOf('.');
        packageName = className.substring(0, lastDot);
        className = className.substring(lastDot + 1);
      }

      if (className != null && packageName != null) {
        String classId = packageName + "." + className;
        // Only add DEFINES edge if the class node exists
        if (unified.getNode(classId) != null) {
          unified.addMethodToClassEdge(methodId, classId);
        }
      }
    }

    return unified;
  }

  /**
   * Normalize a method ID used in edges by applying signature normalization
   */
  private static String normalizeMethodIdInEdge(String methodId) {
    if (methodId == null || !methodId.contains("#")) {
      return methodId;
    }

    // Format: com.package.Class#method(types):returnType
    String[] parts = methodId.split("#", 2);
    if (parts.length == 2) {
      String classPath = parts[0];
      String methodPart = parts[1];

      // Normalize the method part (remove FQN types)
      String normalizedMethod = SignatureNormalizer.normalizeSignature(methodPart);

      return classPath + "#" + normalizedMethod;
    }

    return methodId;
  }

  /**
   * Extract HTTP method from endpoint ID like "endpoint:POST /api/orders"
   */
  private static String extractHttpMethodFromEndpointId(String endpointId) {
    if (endpointId == null || !endpointId.startsWith("endpoint:")) {
      return "GET";
    }
    String withoutPrefix = endpointId.substring("endpoint:".length()).trim();
    String[] parts = withoutPrefix.split(" ", 2);
    return parts.length > 0 ? parts[0] : "GET";
  }

  /**
   * Extract path from endpoint ID like "endpoint:POST /api/orders"
   */
  private static String extractPathFromEndpointId(String endpointId) {
    if (endpointId == null || !endpointId.startsWith("endpoint:")) {
      return "/";
    }
    String withoutPrefix = endpointId.substring("endpoint:".length()).trim();
    String[] parts = withoutPrefix.split(" ", 2);
    return parts.length > 1 ? parts[1] : "/";
  }

  /**
   * Extract topic name from a messaging edge value.
   * May be in format "topic:orders.v1" or "orders.v1"
   */
  private static String extractTopicNameFromMessagingEdge(String value) {
    if (value == null) {
      return "";
    }
    // If it already has topic: prefix, remove it
    if (value.startsWith("topic:")) {
      return value.substring(6);
    }
    // Otherwise return as-is (should be just the topic name)
    return value;
  }
}

