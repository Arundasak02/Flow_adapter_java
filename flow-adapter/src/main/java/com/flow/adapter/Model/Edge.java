package com.flow.adapter.Model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Edge {

  public String id;
  public String from; // node ID
  public String to;   // node ID
  public String type; // CALL, HANDLES, PRODUCES, CONSUMES, ASYNC_HOP
  public Map<String, Object> data = new LinkedHashMap<>();

  public Edge() {
  }

  public Edge(String id, String from, String to, String type) {
    this.id = id;
    this.from = from;
    this.to = to;
    this.type = type;
  }
}

