package com.flow.adapter.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified graph model that uses nodes and edges for the FLOW platform.
 * This is the new format that replaces the legacy format.
 */
public class UnifiedGraphModel {

  public String graphId;
  public List<Node> nodes = new ArrayList<>();
  public List<Edge> edges = new ArrayList<>();

  // Helper maps for quick lookup
  private Map<String, Node> nodeMap = new HashMap<>();
  private int edgeCounter = 0;

  public UnifiedGraphModel() {
  }

  public UnifiedGraphModel(String graphId) {
    this.graphId = graphId;
  }

  /**
   * Get or create a node by ID
   */
  public Node ensureNode(String id, String type, String name) {
    return nodeMap.computeIfAbsent(id, k -> {
      Node n = new Node(id, type, name);
      nodes.add(n);
      return n;
    });
  }

  /**
   * Get an existing node
   */
  public Node getNode(String id) {
    return nodeMap.get(id);
  }

  /**
   * Add an edge to the graph
   */
  public void addEdge(String from, String to, String type) {
    edgeCounter++;
    String edgeId = "e-" + type.toLowerCase() + "-" + edgeCounter;
    addEdge(edgeId, from, to, type);
  }

  /**
   * Add an edge with explicit ID
   */
  public void addEdge(String edgeId, String from, String to, String type) {
    Edge e = new Edge(edgeId, from, to, type);
    edges.add(e);
  }

  /**
   * Create an ENDPOINT node
   */
  public Node addEndpoint(String httpMethod, String path) {
    String id = "endpoint:" + httpMethod + " " + path;
    String name = httpMethod + " " + path;
    Node n = ensureNode(id, "ENDPOINT", name);
    n.data.put("httpMethod", httpMethod);
    n.data.put("path", path);
    return n;
  }

  /**
   * Create a TOPIC node
   */
  public Node addTopic(String topicName) {
    String id = "topic:" + topicName;
    Node n = ensureNode(id, "TOPIC", topicName);
    return n;
  }

  /**
   * Create a METHOD node
   */
  public Node addMethod(String methodId, String methodName, String visibility,
                        String className, String packageName, String moduleName, String signature) {
    String type = "private".equals(visibility) ? "PRIVATE_METHOD" : "METHOD";
    String displayName = className != null ? className + "." + methodName : methodName;
    Node n = ensureNode(methodId, type, displayName);
    n.data.put("visibility", visibility);
    n.data.put("className", className);
    n.data.put("packageName", packageName);
    if (moduleName != null) {
      n.data.put("moduleName", moduleName);
    }
    if (signature != null) {
      n.data.put("signature", signature);
    }
    return n;
  }
}

