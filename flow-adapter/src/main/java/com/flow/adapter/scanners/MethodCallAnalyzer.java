package com.flow.adapter.scanners;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.util.PackageUtil;
import com.flow.adapter.util.SignatureUtil;
import com.flow.adapter.util.VisibilityUtil;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.MethodAmbiguityException;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodCallAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(MethodCallAnalyzer.class);

  public void analyze(GraphModel model, CompilationUnit cu, String fqn, String pkg, String module, MethodDeclaration md) {
    GraphModel.MethodNode node = createMethodNode(model, fqn, pkg, module, md);
    processMethodCalls(model, cu, md, node.id);
  }

  private GraphModel.MethodNode createMethodNode(GraphModel model, String fqn, String pkg, String module, MethodDeclaration md) {
    String sig = SignatureUtil.signatureOf(md);
    String id = fqn + "#" + sig;
    GraphModel.MethodNode node = model.ensureMethod(id);
    node.id = id;
    node.className = fqn;
    node.methodName = md.getNameAsString();
    node.signature = sig;
    node.packageName = pkg;
    node.moduleName = module;
    node.visibility = VisibilityUtil.visibilityOf(md);
    return node;
  }

  private void processMethodCalls(GraphModel model, CompilationUnit cu, MethodDeclaration md, String callerId) {
    md.findAll(MethodCallExpr.class).forEach(call -> processMethodCall(model, cu, call, callerId));
  }

  private void processMethodCall(GraphModel model, CompilationUnit cu, MethodCallExpr call, String callerId) {
    try {
      ResolvedMethodDeclaration resolved = call.resolve();
      GraphModel.MethodNode target = createTargetMethodNode(model, resolved);
      addCallEdge(model, callerId, target.id);
    } catch (UnsolvedSymbolException | MethodAmbiguityException ex) {
      logger.warn("Could not resolve symbol or method ambiguity for call in {}: {}",
          cu.getPrimaryTypeName().orElse("unknown"), ex.getMessage());
    } catch (Exception ex) {
      logger.error("Unexpected error while processing method call in {}: {}",
          cu.getPrimaryTypeName().orElse("unknown"), ex.getMessage(), ex);
    }
  }

  private GraphModel.MethodNode createTargetMethodNode(GraphModel model, ResolvedMethodDeclaration resolved) {
    String className = resolved.declaringType().getQualifiedName();
    String sig = SignatureUtil.signatureOf(resolved);
    String id = className + "#" + sig;

    GraphModel.MethodNode node = model.ensureMethod(id);
    node.id = id;
    node.className = className;
    node.methodName = resolved.getName();
    node.signature = sig;
    node.packageName = extractPackage(className);
    node.moduleName = PackageUtil.deriveModule(node.packageName);
    node.visibility = VisibilityUtil.visibilityOf(resolved);
    return node;
  }

  private String extractPackage(String className) {
    int idx = className.lastIndexOf('.');
    return idx > 0 ? className.substring(0, idx) : "";
  }

  private void addCallEdge(GraphModel model, String fromId, String toId) {
    GraphModel.CallEdge edge = new GraphModel.CallEdge();
    edge.from = fromId;
    edge.to = toId;
    model.calls.add(edge);
  }
}
