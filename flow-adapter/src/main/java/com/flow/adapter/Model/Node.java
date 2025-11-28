package com.flow.adapter.Model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a unified node in the FLOW graph.
 * Node types: ENDPOINT, TOPIC, METHOD, PRIVATE_METHOD, CLASS, SERVICE
 */
public class Node {

  public String id;
  public String type; // ENDPOINT, TOPIC, METHOD, PRIVATE_METHOD, CLASS, SERVICE
  public String name;
  public Map<String, Object> data = new LinkedHashMap<>();

  public Node() {
  }

  public Node(String id, String type, String name) {
    this.id = id;
    this.type = type;
    this.name = name;
  }
}

