package com.flow.adapter.scanners;
import com.flow.adapter.Model.GraphModel; import com.flow.adapter.util.SignatureUtil; import com.flow.adapter.util.VisibilityUtil;
import com.github.javaparser.StaticJavaParser; import com.github.javaparser.ast.CompilationUnit; import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration; import com.github.javaparser.ast.body.MethodDeclaration; import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.MethodAmbiguityException; import com.github.javaparser.resolution.UnsolvedSymbolException; import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver; import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver; import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver; import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.nio.file.*; import java.io.IOException;
public class JavaSourceScanner{
  public JavaSourceScanner(Object cfg){}
  public void scanInto(GraphModel model, Path srcRoot) throws IOException{
    CombinedTypeSolver ts=new CombinedTypeSolver(); ts.add(new ReflectionTypeSolver()); ts.add(new JavaParserTypeSolver(srcRoot));
    JavaSymbolSolver ss=new JavaSymbolSolver(ts); StaticJavaParser.getConfiguration().setSymbolResolver(ss);
    Files.walk(srcRoot).filter(p->p.toString().endsWith(".java")).forEach(p->parseFile(model,p));
  }
  private void parseFile(GraphModel model, Path file){
    try{
      CompilationUnit cu=StaticJavaParser.parse(file);
      cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls->{
        String pkg=cu.getPackageDeclaration().map(pd->pd.getName().toString()).orElse("");
        String fqn=pkg.isEmpty()?cls.getName().asString():pkg+"."+cls.getName().asString();
        String module=deriveModule(pkg);
        for(MethodDeclaration md: cls.getMethods()){
          String sig=SignatureUtil.signatureOf(md); String id=fqn+"#"+sig;
          GraphModel.MethodNode n=model.ensureMethod(id);
          n.id=id; n.className=fqn; n.methodName=md.getNameAsString(); n.signature=sig; n.packageName=pkg; n.moduleName=module; n.visibility=VisibilityUtil.visibilityOf(md);
          md.findAll(MethodCallExpr.class).forEach(call->{ try{
            ResolvedMethodDeclaration t=call.resolve();
            String tClass=t.declaringType().getQualifiedName(); String tSig=SignatureUtil.signatureOf(t); String tId=tClass+"#"+tSig;
            GraphModel.MethodNode tn=model.ensureMethod(tId); tn.id=tId; tn.className=tClass; tn.methodName=t.getName(); tn.signature=tSig;
            int idx=tClass.lastIndexOf('.'); String p=idx>0?tClass.substring(0,idx):""; tn.packageName=p; tn.moduleName=deriveModule(p); tn.visibility="unknown";
            GraphModel.CallEdge e=new GraphModel.CallEdge(); e.from=id; e.to=tId; model.calls.add(e);
          }catch(UnsolvedSymbolException|MethodAmbiguityException ex){} catch(Exception ex){} });
        }
      });
    }catch(Exception e){ System.err.println("Parse fail: "+file+" -> "+e.getMessage()); }
  }
  private String deriveModule(String pkg){ if(pkg==null||pkg.isEmpty()) return ""; String[] parts=pkg.split("\."); if(parts.length>=3) return parts[2]; if(parts.length>=2) return parts[1]; return parts[0]; }
}