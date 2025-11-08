package com.flow.adapter.util;
import com.github.javaparser.ast.body.MethodDeclaration;
public class VisibilityUtil{
  public static String visibilityOf(MethodDeclaration md){
    if(md.isPublic()) return "public"; if(md.isProtected()) return "protected"; if(md.isPrivate()) return "private"; return "package";
  }
}