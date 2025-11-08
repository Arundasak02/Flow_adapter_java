package com.flow.adapter;

import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.util.ConfigLoader;

import java.nio.file.Path;

public interface FlowPlugin {
  void enrich(GraphModel model, Path srcRoot, ConfigLoader config) throws Exception;
}
