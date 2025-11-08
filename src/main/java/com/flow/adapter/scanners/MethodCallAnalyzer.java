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

/**
 * Analyzes method calls within a Java CompilationUnit and populates a GraphModel. This class is
 * responsible for extracting information about called methods and creating call edges.
 */
public class MethodCallAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(MethodCallAnalyzer.class);

  public MethodCallAnalyzer() {
  }

  /**
   * Analyzes a given MethodDeclaration for method calls and populates the GraphModel.
   *
   * @param model  The GraphModel to populate with method call data.
   * @param cu     The CompilationUnit containing the method.
   * @param fqn    The fully qualified name of the class containing the method.
   * @param pkg    The package name of the class containing the method.
   * @param module The module name derived from the package.
   * @param md     The MethodDeclaration to analyze.
   */
  public void analyze(GraphModel model, CompilationUnit cu, String fqn, String pkg, String module,
      MethodDeclaration md) {
    String sig = SignatureUtil.signatureOf(md);
    String id = fqn + "#" + sig;
    GraphModel.MethodNode n = model.ensureMethod(id);
    n.id = id;
    n.className = fqn;
    n.methodName = md.getNameAsString();
    n.signature = sig;
    n.packageName = pkg;
    n.moduleName = module;
    n.visibility = VisibilityUtil.visibilityOf(md);

    md.findAll(MethodCallExpr.class).forEach(call -> {
      try {
        ResolvedMethodDeclaration t = call.resolve();
        String tClass = t.declaringType().getQualifiedName();
        String tSig = SignatureUtil.signatureOf(t);
        String tId = tClass + "#" + tSig;
        GraphModel.MethodNode tn = model.ensureMethod(tId);
        tn.id = tId;
        tn.className = tClass;
        tn.methodName = t.getName();
        tn.signature = tSig;
        int idx = tClass.lastIndexOf('.');
        String p = idx > 0 ? tClass.substring(0, idx) : "";
        tn.packageName = p;
        tn.moduleName = PackageUtil.deriveModule(p);
        tn.visibility = VisibilityUtil.visibilityOf(t);
        GraphModel.CallEdge e = new GraphModel.CallEdge();
        e.from = id;
        e.to = tId;
        model.calls.add(e);
      } catch (UnsolvedSymbolException | MethodAmbiguityException ex) {
        logger.warn("Could not resolve symbol or method ambiguity for call in {}: {}",
            cu.getPrimaryTypeName().orElse("unknown"), ex.getMessage());
      } catch (Exception ex) {
        logger.error("Unexpected error while processing method call in {}: {}",
            cu.getPrimaryTypeName().orElse("unknown"), ex.getMessage(), ex);
      }
    });
  }
}
