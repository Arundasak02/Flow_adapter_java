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
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaScanner {

  private static final Logger logger = LoggerFactory.getLogger(KafkaScanner.class);
  private static final Set<String> ANN = new HashSet<>(Arrays.asList("KafkaListener", "Input", "Output"));

  private final ConfigLoader cfg;

  public KafkaScanner(ConfigLoader cfg) {
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
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> processClass(model, cu, cls));
    } catch (Exception e) {
      logger.warn("Kafka scanner failed for file {}", file, e);
    }
  }

  private void processClass(GraphModel model, CompilationUnit cu, ClassOrInterfaceDeclaration cls) {
    String pkg = extractPackage(cu);
    String className = buildClassName(pkg, cls.getName().asString());

    cls.getMethods().forEach(md -> {
      processKafkaAnnotations(model, md, className);
      processKafkaTemplateCalls(model, md, className);
    });
  }

  private void processKafkaAnnotations(GraphModel model, MethodDeclaration md, String className) {
    findKafkaAnnotation(md).ifPresent(ann -> {
      String topic = extractTopic(ann);
      if (!topic.isEmpty()) {
        addMessagingEdge(model, md, className, topic, determineKind(ann.getName().getIdentifier()));
      }
    });
  }

  private Optional<AnnotationExpr> findKafkaAnnotation(MethodDeclaration md) {
    return md.getAnnotations().stream()
        .filter(a -> ANN.contains(a.getName().getIdentifier()))
        .findFirst();
  }

  private void processKafkaTemplateCalls(GraphModel model, MethodDeclaration md, String className) {
    md.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).stream()
        .filter(this::isKafkaTemplateSend)
        .forEach(mce -> processKafkaSend(model, mce, md, className));
  }

  private boolean isKafkaTemplateSend(com.github.javaparser.ast.expr.MethodCallExpr mce) {
    return "send".equals(mce.getNameAsString())
        && mce.getScope().isPresent()
        && mce.getScope().get().toString().contains("kafkaTemplate");
  }

  private void processKafkaSend(GraphModel model, com.github.javaparser.ast.expr.MethodCallExpr mce,
                                 MethodDeclaration md, String className) {
    if (mce.getArguments().isEmpty()) return;

    String topicArg = mce.getArguments().get(0).toString();
    String topic = str(topicArg);

    if (!topic.isEmpty()) {
      addMessagingEdge(model, md, className, topic, "produces");
    }
  }

  private void addMessagingEdge(GraphModel model, MethodDeclaration md, String className,
                                 String topic, String kind) {
    String sig = SignatureUtil.signatureOf(md);
    String methodId = className + "#" + sig;
    GraphModel.TopicNode topicNode = model.ensureTopic(topic);
    model.addMessagingEdge(methodId, topicNode.id, kind);
    logger.info("Added Kafka {} edge: {} -> {}", kind, methodId, topicNode.id);
  }

  private String determineKind(String annName) {
    return annName.equals("Output") ? "produces" : "consumes";
  }

  private String extractTopic(AnnotationExpr ann) {
    if (ann.isSingleMemberAnnotationExpr()) {
      return str(ann.asSingleMemberAnnotationExpr().getMemberValue().toString());
    }
    if (ann.isNormalAnnotationExpr()) {
      return extractTopicFromPairs(ann.asNormalAnnotationExpr().getPairs());
    }
    return "";
  }

  private String extractTopicFromPairs(java.util.List<MemberValuePair> pairs) {
    for (MemberValuePair p : pairs) {
      if (isTopicAttribute(p.getNameAsString())) {
        String topic = extractTopicValue(p.getValue());
        if (!topic.isEmpty()) return topic;
      }
    }
    return "";
  }

  private boolean isTopicAttribute(String name) {
    return name.equals("topics") || name.equals("value");
  }

  private String extractTopicValue(Expression val) {
    if (val.isStringLiteralExpr()) {
      return str(val.asStringLiteralExpr().asString());
    }
    if (val.isArrayInitializerExpr()) {
      return val.asArrayInitializerExpr().getValues().stream()
          .filter(Expression::isStringLiteralExpr)
          .findFirst()
          .map(e -> str(e.asStringLiteralExpr().asString()))
          .orElse("");
    }
    return "";
  }

  private String extractPackage(CompilationUnit cu) {
    return cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
  }

  private String buildClassName(String pkg, String simpleName) {
    return pkg.isEmpty() ? simpleName : pkg + "." + simpleName;
  }

  private String str(String raw) {
    String s = raw.startsWith("\"") && raw.endsWith("\"")
        ? raw.substring(1, raw.length() - 1)
        : raw;
    return cfg != null ? cfg.resolvePlaceholders(s) : s;
  }
}
