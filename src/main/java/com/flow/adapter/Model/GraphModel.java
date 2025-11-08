package com.flow.adapter.Model;
import java.util.*;
public class GraphModel {
  public static class MethodNode{ public String id,className,methodName,signature,visibility,packageName,moduleName; }
  public static class EndpointNode{ public String id,httpMethod,path,produces,consumes; }
  public static class TopicNode{ public String id,name; }
  public static class CallEdge{ public String from,to,kind="calls"; }
  public static class EndpointEdge{ public String fromEndpoint,toMethod,kind="handles"; }
  public static class MessagingEdge{ public String from,to,kind; }
  public String projectId,schema;
  public Map<String,MethodNode> methods=new LinkedHashMap<>();
  public Map<String,EndpointNode> endpoints=new LinkedHashMap<>();
  public Map<String,TopicNode> topics=new LinkedHashMap<>();
  public java.util.List<CallEdge> calls=new java.util.ArrayList<>();
  public java.util.List<EndpointEdge> endpointEdges=new java.util.ArrayList<>();
  public java.util.List<MessagingEdge> messaging=new java.util.ArrayList<>();
  public MethodNode ensureMethod(String id){ return methods.computeIfAbsent(id,k->new MethodNode()); }
  public EndpointNode ensureEndpoint(String id){ return endpoints.computeIfAbsent(id,k->new EndpointNode()); }
  public TopicNode ensureTopic(String name){ String id="topic:"+name; return topics.computeIfAbsent(id,k->{TopicNode t=new TopicNode(); t.id=id; t.name=name; return t;}); }
}