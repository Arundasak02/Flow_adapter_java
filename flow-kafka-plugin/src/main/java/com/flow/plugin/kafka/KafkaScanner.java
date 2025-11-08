package com.flow.plugin.kafka;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.util.ConfigLoader;
import com.flow.adapter.util.SignatureUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class KafkaScanner {

  private static final Set<String> ANN = new HashSet<>(
      Arrays.asList("KafkaListener", "Input", "Output"));
  private final ConfigLoader cfg;

  public KafkaScanner(ConfigLoader cfg) {
    this.cfg = cfg;
  }

  public void scanInto(GraphModel model, Path srcRoot) throws IOException {
    Files.walk(srcRoot).filter(p -> p.toString().endsWith(".java"))
        .forEach(p -> parseFile(model, p));
  }

  private void parseFile(GraphModel model, Path file) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
        for (MethodDeclaration md : cls.getMethods()) {
          Optional<AnnotationExpr> opt = md.getAnnotations().stream()
              .filter(a -> ANN.contains(a.getName().getIdentifier())).findFirst();
          if (!opt.isPresent()) {
            continue;
          }
          AnnotationExpr ann = opt.get();
          String topic = extractTopic(ann);
          if (topic.isEmpty()) {
            continue;
          }
          String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
          String clsName =
              pkg.isEmpty() ? cls.getName().asString() : pkg + "." + cls.getName().asString();
          String mid = SignatureUtil.signatureOf(md);
          String tid = "topic:" + topic;
          GraphModel.TopicNode tn = model.ensureTopic(tid);
          tn.id = tid;
          tn.name = topic;
          GraphModel.MessagingEdge edge = new GraphModel.MessagingEdge();
          edge.from = mid;
          edge.to = tid;
          model.messaging.add(edge);
        }
      });
    } catch (Exception e) {
      System.err.println("Kafka fail: " + file + " -> " + e.getMessage());
    }
  }

  private String extractTopic(AnnotationExpr ann) {
    if (ann.isSingleMemberAnnotationExpr()) {
      String v = ann.asSingleMemberAnnotationExpr().getMemberValue().toString();
      return str(v);
    }
    if (ann.isNormalAnnotationExpr()) {
      for (MemberValuePair p : ann.asNormalAnnotationExpr().getPairs()) {
        String k = p.getNameAsString();
        if (k.equals("topics") || k.equals("value")) {
          Expression val = p.getValue();
          if (val.isStringLiteralExpr()) {
            return str(val.asStringLiteralExpr().asString());
          } else if (val.isArrayInitializerExpr()) {
            Optional<Expression> fst = val.asArrayInitializerExpr().getValues().stream().findFirst();
            if (fst.isPresent() && fst.get().isStringLiteralExpr()) {
              return str(fst.get().asStringLiteralExpr().asString());
            }
          }
        }
      }
    }
    return "";
  }

  private String str(String raw) {
    String s = raw;
    if (s.startsWith("\"") && s.endsWith("\"")) {
      s = s.substring(1, s.length() - 1);
    }
    return cfg != null ? cfg.resolvePlaceholders(s) : s;
  }
}
