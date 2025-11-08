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

/**
 * Scans Java source files to build a graph model of methods and their calls. It uses JavaParser for
 * AST parsing and JavaSymbolSolver for symbol resolution.
 */
public class JavaSourceScanner implements SourceCodeAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(JavaSourceScanner.class);

  public JavaSourceScanner() {
  }

  /**
   * Analyzes Java source code within a given root directory and populates a GraphModel.
   *
   * @param model   The GraphModel to populate with scanned data.
   * @param srcRoot The root directory containing Java source files.
   * @throws IOException If an I/O error occurs during file traversal.
   */
  @Override
  public void analyze(GraphModel model, Path srcRoot) throws IOException {
    // Set up the symbol solver for accurate type and method resolution
    CombinedTypeSolver ts = new CombinedTypeSolver();
    ts.add(new ReflectionTypeSolver());
    ts.add(new JavaParserTypeSolver(srcRoot));
    JavaSymbolSolver ss = new JavaSymbolSolver(ts);

    // Configure StaticJavaParser with the symbol resolver
    ParserConfiguration parserConfiguration = new ParserConfiguration();
    parserConfiguration.setSymbolResolver(ss);
    StaticJavaParser.setConfiguration(parserConfiguration);

    // Initialize the MethodCallAnalyzer to handle method call extraction
    MethodCallAnalyzer methodCallAnalyzer = new MethodCallAnalyzer();

    // Walk through all Java files in the source root and parse each one
    try (Stream<Path> walk = Files.walk(srcRoot)) {
      walk.filter(p -> p.toString().endsWith(".java"))
          .forEach(p -> parseFile(model, p, methodCallAnalyzer));
    }
  }

  /**
   * Parses a single Java file, extracts class and method information, and delegates method call
   * analysis to MethodCallAnalyzer.
   *
   * @param model              The GraphModel to populate.
   * @param file               The Java source file to parse.
   * @param methodCallAnalyzer The analyzer responsible for extracting method call details.
   */
  private void parseFile(GraphModel model, Path file, MethodCallAnalyzer methodCallAnalyzer) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String fqn =
            pkg.isEmpty() ? cls.getName().asString() : pkg + "." + cls.getName().asString();
        String module = PackageUtil.deriveModule(pkg);
        for (MethodDeclaration md : cls.getMethods()) {
          // Analyze each method for calls and populate the graph model
          methodCallAnalyzer.analyze(model, cu, fqn, pkg, module, md);
        }
      });
    } catch (Exception e) {
      logger.error("Parse fail: {} -> {}", file, e.getMessage(), e);
    }
  }
}