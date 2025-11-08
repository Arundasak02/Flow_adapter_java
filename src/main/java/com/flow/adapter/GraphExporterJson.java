package com.flow.adapter;
import com.fasterxml.jackson.databind.ObjectMapper; import com.fasterxml.jackson.databind.SerializationFeature;
import com.flow.adapter.Model.GraphModel;
import java.io.IOException; import java.nio.file.Path;
public class GraphExporterJson {
  private final ObjectMapper mapper=new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  public void write(GraphModel m, Path out) throws IOException{ mapper.writeValue(out.toFile(), m); }
}