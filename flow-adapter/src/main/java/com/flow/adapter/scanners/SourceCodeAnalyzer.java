package com.flow.adapter.scanners;

import com.flow.adapter.Model.GraphModel;
import java.io.IOException;
import java.nio.file.Path;

public interface SourceCodeAnalyzer {

  void analyze(GraphModel model, Path srcRoot) throws IOException;
}

