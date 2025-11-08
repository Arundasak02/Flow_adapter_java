package com.flow.adapter.scanners;
import com.flow.adapter.Model.GraphModel; import com.flow.adapter.util.ConfigLoader; import com.flow.adapter.util.SignatureUtil;
import com.github.javaparser.StaticJavaParser; import com.github.javaparser.ast.CompilationUnit; import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration; import com.github.javaparser.ast.body.MethodDeclaration; import com.github.javaparser.ast.expr.*;
import java.nio.file.*; import java.io.IOException; import java.util.*;
public class SpringEndpointScanner{
  private final ConfigLoader cfg; private static final Set<String> ANN=new HashSet<>(Arrays.asList("GetMapping","PostMapping","PutMapping","DeleteMapping","PatchMapping","RequestMapping"));
  public SpringEndpointScanner(ConfigLoader cfg){ this.cfg=cfg; }
  public void scanInto(GraphModel model, Path srcRoot) throws IOException{
    Files.walk(srcRoot).filter(p->p.toString().endsWith(".java")).forEach(p->parseFile(model,p));
  }
  private void parseFile(GraphModel model, Path file){
    try{
      CompilationUnit cu=StaticJavaParser.parse(file);
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls->{
        String base=extractPath(cls.getAnnotations());
        for(MethodDeclaration md: cls.getMethods()){
          Optional<AnnotationExpr> opt=md.getAnnotations().stream().filter(a->ANN.contains(a.getName().getIdentifier())).findFirst();
          if(!opt.isPresent()) continue;
          String http=method(opt.get().getName().getIdentifier());
          String mpath=extractPath(md.getAnnotations());
          String path=normalize(base, mpath);
          String eid="endpoint:"+http+" "+path;
          GraphModel.EndpointNode ep=model.ensureEndpoint(eid); ep.id=eid; ep.httpMethod=http; ep.path=path;
          String pkg=cu.getPackageDeclaration().map(pd->pd.getName().toString()).orElse("");
          String clsName=pkg.isEmpty()?cls.getName().asString():pkg+"."+cls.getName().asString();
          String sig=SignatureUtil.signatureOf(md); String mid=clsName+"#"+sig;
          GraphModel.EndpointEdge edge=new GraphModel.EndpointEdge(); edge.fromEndpoint=eid; edge.toMethod=mid; model.endpointEdges.add(edge);
        }
      });
    }catch(Exception e){ System.err.println("EP fail: "+file+" -> "+e.getMessage()); }
  }
  private String method(String ann){ switch(ann){ case "GetMapping": return "GET"; case "PostMapping": return "POST"; case "PutMapping": return "PUT"; case "DeleteMapping": return "DELETE"; case "PatchMapping": return "PATCH"; default: return "REQUEST"; } }
  private String extractPath(List<AnnotationExpr> anns){
    for(AnnotationExpr a: anns){ String n=a.getName().getIdentifier();
      if(n.equals("RequestMapping")||n.endsWith("Mapping")){
        if(a.isSingleMemberAnnotationExpr()){ String v=a.asSingleMemberAnnotationExpr().getMemberValue().toString(); return str(v); }
        if(a.isNormalAnnotationExpr()){ for(MemberValuePair p: a.asNormalAnnotationExpr().getPairs()){ String k=p.getNameAsString(); if(k.equals("value")||k.equals("path")) return str(p.getValue().toString()); }
        }
      }
    } return "";
  }
  private String str(String raw){ String s=raw; if(s.startsWith(""")&&s.endsWith(""")) s=s.substring(1,s.length()-1); return cfg!=null?cfg.resolvePlaceholders(s):s; }
  private String normalize(String a, String b){ String x=a==null?"":a.trim(); String y=b==null?"":b.trim(); if(!x.startsWith("/")) x="/"+x; if(x.endsWith("/")) x=x.substring(0,x.length()-1); if(!y.isEmpty()&&!y.startsWith("/")) y="/"+y; return (x+y).replaceAll("//+","/"); }
}