package com.flow.adapter.util;
import org.yaml.snakeyaml.Yaml; import java.io.*; import java.nio.file.*; import java.util.*;
@SuppressWarnings("unchecked")
public class ConfigLoader{
  private final Map<String,String> values=new HashMap<>();
  public void loadDirectory(Path dir) throws IOException{
    if(!Files.exists(dir)) return;
    try(DirectoryStream<Path> ds=Files.newDirectoryStream(dir)){ for(Path p:ds){
      String n=p.getFileName().toString();
      if(n.endsWith(".properties")) loadProperties(p);
      if(n.endsWith(".yml")||n.endsWith(".yaml")) loadYaml(p);
    }}
  }
  private void loadProperties(Path f) throws IOException{
    Properties props=new Properties(); try(InputStream in=Files.newInputStream(f)){ props.load(in); }
    for(String k:props.stringPropertyNames()){ values.put(k, props.getProperty(k)); }
  }
  private void loadYaml(Path f) throws IOException{
    Yaml yaml=new Yaml(); try(InputStream in=Files.newInputStream(f)){ Object data=yaml.load(in); flatten("", data); }
  }
  private void flatten(String prefix, Object o){
    if(o instanceof Map){ Map<Object,Object> m=(Map<Object,Object>)o;
      for(Map.Entry<Object,Object> e:m.entrySet()){ String k=prefix.isEmpty()?String.valueOf(e.getKey()):prefix+"."+e.getKey(); flatten(k,e.getValue()); }
    } else { values.put(prefix, String.valueOf(o)); }
  }
  public String resolvePlaceholders(String v){
    if(v==null) return null; String out=v;
    for(int i=0;i<5;i++){ int s=out.indexOf("${"); if(s<0) break; int e=out.indexOf("}", s+2); if(e<0) break;
      String key=out.substring(s+2,e); String rep=values.getOrDefault(key,""); out=out.substring(0,s)+rep+out.substring(e+1);
    } return out;
  }
}