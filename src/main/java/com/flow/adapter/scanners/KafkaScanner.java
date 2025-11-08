package com.flow.adapter.scanners;
import com.flow.adapter.Model.GraphModel; import com.flow.adapter.util.ConfigLoader; import com.flow.adapter.util.SignatureUtil;
import com.github.javaparser.StaticJavaParser; import com.github.javaparser.ast.CompilationUnit; import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration; import com.github.javaparser.ast.body.MethodDeclaration; import com.github.javaparser.ast.expr.*;
import java.nio.file.*; import java.io.IOException;
import java.util.stream.Collectors;
public class KafkaScanner{
  private final ConfigLoader cfg; public KafkaScanner(ConfigLoader cfg){ this.cfg=cfg; }
  public void scanInto(GraphModel model, Path srcRoot) throws IOException{
    Files.walk(srcRoot).filter(p->p.toString().endsWith(".java")).forEach(p->parseFile(model,p));
  }
  private void parseFile(GraphModel model, Path file){
    try{
      CompilationUnit cu=StaticJavaParser.parse(file);
      cu.findAll(MethodDeclaration.class).forEach(md->{ // consumers
        java.util.List<String> topics=extractTopics(md.getAnnotations());
        if(!topics.isEmpty()){
          String pkg=cu.getPackageDeclaration().map(pd->pd.getName().toString()).orElse("");
          String clsName=pkg; java.util.Optional<ClassOrInterfaceDeclaration> cls=md.findAncestor(ClassOrInterfaceDeclaration.class);
          if(cls.isPresent()){ String cn=cls.get().getNameAsString(); clsName=pkg.isEmpty()?cn:pkg+"."+cn; }
          String sig=SignatureUtil.signatureOf(md); String mid=clsName+"#"+sig;
          for(String t: topics){ String name=resolve(t); GraphModel.TopicNode tn=model.ensureTopic(name);
            GraphModel.MessagingEdge e=new GraphModel.MessagingEdge(); e.from=tn.id; e.to=mid; e.kind="consumes"; model.messaging.add(e); }
        }
      });
      cu.findAll(MethodCallExpr.class).forEach(call->{ // producers
        if(!call.getName().asString().equals("send")) return;
        if(call.getArguments().isEmpty()) return;
        Expression first=call.getArgument(0);
        if(first.isStringLiteralExpr()){
          String topic=resolve(first.asStringLiteralExpr().asString()); GraphModel.TopicNode tn=model.ensureTopic(topic);
          java.util.Optional<MethodDeclaration> enc=call.findAncestor(MethodDeclaration.class);
          if(enc.isPresent()){
            MethodDeclaration md=enc.get();
            String pkg=cu.getPackageDeclaration().map(pd->pd.getName().toString()).orElse("");
            String clsName=pkg; java.util.Optional<ClassOrInterfaceDeclaration> cls=md.findAncestor(ClassOrInterfaceDeclaration.class);
            if(cls.isPresent()){ String cn=cls.get().getNameAsString(); clsName=pkg.isEmpty()?cn:pkg+"."+cn; }
            String sig=SignatureUtil.signatureOf(md); String mid=clsName+"#"+sig;
            GraphModel.MessagingEdge e=new GraphModel.MessagingEdge(); e.from=mid; e.to=tn.id; e.kind="publishes"; model.messaging.add(e);
          }
        }
      });
    }catch(Exception e){ System.err.println("Kafka fail: "+file+" -> "+e.getMessage()); }
  }
  private java.util.List<String> extractTopics(java.util.List<AnnotationExpr> anns){
    for(AnnotationExpr a: anns){
      if(a.getName().getIdentifier().equals("KafkaListener")){
        if(a.isSingleMemberAnnotationExpr()) return toList(a.asSingleMemberAnnotationExpr().getMemberValue());
        if(a.isNormalAnnotationExpr()) for(MemberValuePair p: a.asNormalAnnotationExpr().getPairs()) if(p.getNameAsString().equals("topics")) return toList(p.getValue());
      }
    } return java.util.Collections.emptyList();
  }
  private java.util.List<String> toList(Expression v){
    if(v.isStringLiteralExpr()) return java.util.Collections.singletonList(v.asStringLiteralExpr().asString());
    if(v.isArrayInitializerExpr()) return v.asArrayInitializerExpr().getValues().stream().filter(Expression::isStringLiteralExpr).map(e->e.asStringLiteralExpr().asString()).collect(Collectors.toList());
    return java.util.Collections.emptyList();
  }
  private String resolve(String raw){ return cfg!=null?cfg.resolvePlaceholders(raw):raw; }
}