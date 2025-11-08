package com.flow.adapter.util;

public class PackageUtil {

  /**
   * Derives a module name from a given package name. The logic extracts the third part of the
   * package if available, otherwise the second, or the first. Returns an empty string if the
   * package is null or empty.
   *
   * @param pkg The package name.
   * @return The derived module name.
   */
  public static String deriveModule(String pkg) {
    if (pkg == null || pkg.isEmpty()) {
      return "";
    }
    String[] parts = pkg.split("\\.");
    if (parts.length == 0) {
      return "";
    }
    if (parts.length >= 3) {
      return parts[2];
    }
    if (parts.length >= 2) {
      return parts[1];
    }
    return parts[0];
  }
}

