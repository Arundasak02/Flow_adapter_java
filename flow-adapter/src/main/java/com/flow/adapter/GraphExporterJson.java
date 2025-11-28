package com.flow.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.flow.adapter.Model.GraphModel;
import com.flow.adapter.Model.UnifiedGraphModel;
import com.flow.adapter.Model.GraphModelConverter;
import java.io.IOException;
import java.nio.file.Path;

public class GraphExporterJson {

  private static final ObjectMapper mapper = createMapper();

  private static ObjectMapper createMapper() {
    ObjectMapper m = JsonMapper.builder().build();
    m.setDefaultPropertyInclusion(Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
    m.enable(SerializationFeature.INDENT_OUTPUT);
    return m;
  }

  /**
   * Write legacy GraphModel in unified format (nodes + edges)
   */
  public void write(GraphModel m, Path out) throws IOException {
    // Convert to unified format
    UnifiedGraphModel unified = GraphModelConverter.convert(m);
    mapper.writeValue(out.toFile(), unified);
  }

  /**
   * Write unified graph model directly
   */
  public void writeUnified(UnifiedGraphModel m, Path out) throws IOException {
    mapper.writeValue(out.toFile(), m);
  }
}