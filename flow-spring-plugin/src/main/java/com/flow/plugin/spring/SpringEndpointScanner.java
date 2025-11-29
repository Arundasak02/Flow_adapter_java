package com.flow.plugin.spring;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.util.ConfigLoader;
import com.flow.adapter.util.SignatureUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringEndpointScanner {

  private static final Logger logger = LoggerFactory.getLogger(SpringEndpointScanner.class);

  private static final Set<String> ANN = new HashSet<>(
      Arrays.asList("GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping",
          "RequestMapping"));
  private final ConfigLoader cfg;

  public SpringEndpointScanner(ConfigLoader cfg) {
    this.cfg = cfg;
  }

  public void scanInto(GraphModel model, Path srcRoot) throws IOException {
    try (java.util.stream.Stream<java.nio.file.Path> stream = Files.walk(srcRoot)) {
      stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> parseFile(model, p));
    }
  }

  private void parseFile(GraphModel model, Path file) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
        String base = extractPath(cls.getAnnotations());
        for (MethodDeclaration md : cls.getMethods()) {
          Optional<AnnotationExpr> opt = md.getAnnotations().stream()
              .filter(a -> ANN.contains(a.getName().getIdentifier())).findFirst();
          if (opt.isEmpty()) {
            continue;
          }
          String http = method(opt.get().getName().getIdentifier());
          String mpath = extractPath(md.getAnnotations());
          String path = normalize(base, mpath);
          String eid = "endpoint:" + http + " " + path;
          GraphModel.EndpointNode ep = model.ensureEndpoint(eid);
          ep.id = eid;
          ep.httpMethod = http;
          ep.path = path;
          // Extract produces/consumes from method annotations first, then fall back to class annotations
          List<String> produces = extractMedia(md.getAnnotations());
          if (produces == null || produces.isEmpty()) {
            produces = extractMedia(cls.getAnnotations());
          }
          List<String> consumes = extractConsumes(md.getAnnotations());
          if (consumes == null || consumes.isEmpty()) {
            consumes = extractConsumes(cls.getAnnotations());
          }
          ep.produces = (produces == null || produces.isEmpty()) ? null : produces;
          ep.consumes = (consumes == null || consumes.isEmpty()) ? null : consumes;
          String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
          String clsName =
              pkg.isEmpty() ? cls.getName().asString() : pkg + "." + cls.getName().asString();
          String sig = SignatureUtil.signatureOf(md);
          String mid = clsName + "#" + sig;
          GraphModel.EndpointEdge edge = new GraphModel.EndpointEdge();
          edge.fromEndpoint = eid;
          edge.toMethod = mid;
          model.endpointEdges.add(edge);
        }
      });
    } catch (Exception e) {
      logger.warn("Endpoint scanning failed for file {}", file, e);
    }
  }

  private String method(String ann) {
    return switch (ann) {
      case "GetMapping" -> "GET";
      case "PostMapping" -> "POST";
      case "PutMapping" -> "PUT";
      case "DeleteMapping" -> "DELETE";
      case "PatchMapping" -> "PATCH";
      default -> "REQUEST";
    };
  }

  private String extractPath(List<AnnotationExpr> anns) {
    for (AnnotationExpr a : anns) {
      String n = a.getName().getIdentifier();
      if (n.equals("RequestMapping") || n.endsWith("Mapping")) {
        if (a.isSingleMemberAnnotationExpr()) {
          String v = a.asSingleMemberAnnotationExpr().getMemberValue().toString();
          return str(v);
        }
        if (a.isNormalAnnotationExpr()) {
          for (MemberValuePair p : a.asNormalAnnotationExpr().getPairs()) {
            String k = p.getNameAsString();
            if (k.equals("value") || k.equals("path")) {
              return str(p.getValue().toString());
            }
          }
        }
      }
    }
    return "";
  }

  private List<String> extractMedia(List<AnnotationExpr> anns) {
    for (AnnotationExpr a : anns) {
      String n = a.getName().getIdentifier();
      if (n.equals("RequestMapping") || n.endsWith("Mapping")) {
        if (a.isNormalAnnotationExpr()) {
          for (MemberValuePair p : a.asNormalAnnotationExpr().getPairs()) {
            String k = p.getNameAsString();
            if (k.equals("produces")) {
              // value can be a string literal or an array initializer; collect all string literals
              List<String> out = new ArrayList<>();
              if (p.getValue().isStringLiteralExpr()) {
                out.add(str(p.getValue().asStringLiteralExpr().asString()));
              } else if (p.getValue().isArrayInitializerExpr()) {
                for (com.github.javaparser.ast.expr.Expression e : p.getValue().asArrayInitializerExpr().getValues()) {
                  if (e.isStringLiteralExpr()) out.add(str(e.asStringLiteralExpr().asString()));
                }
              } else {
                out.add(str(p.getValue().toString()));
              }
              return out.isEmpty() ? null : out;
            }
          }
        }
      }
    }
    return null;
  }

  private List<String> extractConsumes(List<AnnotationExpr> anns) {
    for (AnnotationExpr a : anns) {
      String n = a.getName().getIdentifier();
      if (n.equals("RequestMapping") || n.endsWith("Mapping")) {
        if (a.isNormalAnnotationExpr()) {
          for (MemberValuePair p : a.asNormalAnnotationExpr().getPairs()) {
            String k = p.getNameAsString();
            if (k.equals("consumes")) {
              List<String> out = new ArrayList<>();
              if (p.getValue().isStringLiteralExpr()) {
                out.add(str(p.getValue().asStringLiteralExpr().asString()));
              } else if (p.getValue().isArrayInitializerExpr()) {
                for (com.github.javaparser.ast.expr.Expression e : p.getValue().asArrayInitializerExpr().getValues()) {
                  if (e.isStringLiteralExpr()) out.add(str(e.asStringLiteralExpr().asString()));
                }
              } else {
                out.add(str(p.getValue().toString()));
              }
              return out.isEmpty() ? null : out;
            }
          }
        }
      }
    }
    return null;
  }

  private String normalize(String base, String path) {
    if (base.isEmpty()) {
      return path;
    }
    if (path.isEmpty()) {
      return base;
    }
    if (base.endsWith("/") || path.startsWith("/")) {
      return base + path;
    }
    return base + "/" + path;
  }

  private String str(String raw) {
    String s = raw;
    if (s.startsWith("\"") && s.endsWith("\"")) {
      s = s.substring(1, s.length() - 1);
    }
    return cfg != null ? cfg.resolvePlaceholders(s) : s;
  }
}
