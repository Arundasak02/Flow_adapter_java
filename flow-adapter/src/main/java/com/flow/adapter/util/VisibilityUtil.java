package com.flow.adapter.util;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

public class VisibilityUtil {

  public static String visibilityOf(MethodDeclaration md) {
    if (md.isPublic()) {
      return "public";
    }
    if (md.isProtected()) {
      return "protected";
    }
    if (md.isPrivate()) {
      return "private";
    }
    return "package";
  }

  public static String visibilityOf(ResolvedMethodDeclaration rmd) {
    AccessSpecifier as = rmd.accessSpecifier();
    if (as == AccessSpecifier.PUBLIC) {
      return "public";
    }
    if (as == AccessSpecifier.PROTECTED) {
      return "protected";
    }
    if (as == AccessSpecifier.PRIVATE) {
      return "private";
    }
    return "package";
  }
}