package com.flow.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.flow.adapter.Model.GraphModel;
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

  public void write(GraphModel m, Path out) throws IOException {
    mapper.writeValue(out.toFile(), m);
  }
}