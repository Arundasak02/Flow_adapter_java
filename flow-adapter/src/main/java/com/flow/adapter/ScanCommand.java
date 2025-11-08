package com.flow.adapter;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.scanners.JavaSourceScanner;
import com.flow.adapter.util.ConfigLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "scan", description = "Scan Java sources to produce GEF JSON (methods,endpoints,kafka).")
public class ScanCommand implements Runnable {

  @Option(names = "--src", required = true)
  private String src;
  @Option(names = "--config")
  private String configDir;
  @Option(names = "--out")
  private String out;
  @Option(names = "--project", required = true)
  private String projectId;

  public void run() {
    try {
      Path srcRoot = Paths.get(src);
      if (!Files.exists(srcRoot)) {
        throw new IllegalArgumentException("Missing src: " + srcRoot);
      }
      Path cfgPath = Paths.get(configDir != null ? configDir : "src/main/resources");
      ConfigLoader config = new ConfigLoader(cfgPath);
      GraphModel model = new GraphModel();
      model.projectId = projectId;
      model.schema = "gef:1.1";
      new JavaSourceScanner().analyze(model, srcRoot);

      ServiceLoader<FlowPlugin> plugins = ServiceLoader.load(FlowPlugin.class);
      for (FlowPlugin p : plugins) {
        System.out.println("Running plugin: " + p.getClass().getName());
        p.enrich(model, srcRoot, config);
      }

      Path outPath = out != null ? Paths.get(out) : Paths.get("target/graph.json");
      Files.createDirectories(outPath.getParent());
      new GraphExporterJson().write(model, outPath);
      System.out.println("Graph written to: " + outPath.toAbsolutePath());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}