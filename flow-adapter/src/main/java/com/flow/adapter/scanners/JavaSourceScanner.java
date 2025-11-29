package com.flow.adapter.scanners;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.util.PackageUtil;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSourceScanner implements SourceCodeAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(JavaSourceScanner.class);

  @Override
  public void analyze(GraphModel model, Path srcRoot) throws IOException {
    configureParser(srcRoot);
    MethodCallAnalyzer analyzer = new MethodCallAnalyzer();
    scanJavaFiles(model, srcRoot, analyzer);
  }

  private void configureParser(Path srcRoot) {
    CombinedTypeSolver solver = createTypeSolver(srcRoot);
    ParserConfiguration config = new ParserConfiguration();
    config.setSymbolResolver(new JavaSymbolSolver(solver));
    StaticJavaParser.setConfiguration(config);
  }

  private CombinedTypeSolver createTypeSolver(Path srcRoot) {
    CombinedTypeSolver solver = new CombinedTypeSolver();
    solver.add(new ReflectionTypeSolver());
    solver.add(new JavaParserTypeSolver(srcRoot));
    return solver;
  }

  private void scanJavaFiles(GraphModel model, Path srcRoot, MethodCallAnalyzer analyzer) throws IOException {
    try (Stream<Path> walk = Files.walk(srcRoot)) {
      walk.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> parseFile(model, p, analyzer));
    }
  }

  private void parseFile(GraphModel model, Path file, MethodCallAnalyzer analyzer) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);
      processClasses(model, cu, analyzer);
    } catch (Exception e) {
      logger.error("Parse fail: {} -> {}", file, e.getMessage(), e);
    }
  }

  private void processClasses(GraphModel model, CompilationUnit cu, MethodCallAnalyzer analyzer) {
    cu.findAll(ClassOrInterfaceDeclaration.class)
        .forEach(cls -> processClass(model, cu, cls, analyzer));
  }

  private void processClass(GraphModel model, CompilationUnit cu, ClassOrInterfaceDeclaration cls, MethodCallAnalyzer analyzer) {
    String pkg = extractPackage(cu);
    String fqn = buildFqn(pkg, cls.getName().asString());
    String module = PackageUtil.deriveModule(pkg);
    cls.getMethods().forEach(md -> analyzer.analyze(model, cu, fqn, pkg, module, md));
  }

  private String extractPackage(CompilationUnit cu) {
    return cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
  }

  private String buildFqn(String pkg, String className) {
    return pkg.isEmpty() ? className : pkg + "." + className;
  }
}