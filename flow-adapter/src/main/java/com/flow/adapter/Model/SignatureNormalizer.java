package com.flow.adapter.Model;

import java.util.regex.Pattern;

/**
 * Utility class to normalize method signatures and IDs to ensure consistency.
 * Removes fully-qualified type names and uses simple type names instead.
 * Example: "placeOrder(java.lang.String):java.lang.String" -> "placeOrder(String):String"
 */
public class SignatureNormalizer {

  private static final Pattern FQN_PATTERN = Pattern.compile("\\b[a-z][a-z0-9]*(?:\\.[a-z0-9$_]+)*\\.([A-Z][a-zA-Z0-9$_]*)\\b");

  /**
   * Normalize a method signature by removing fully-qualified type names.
   * Converts: "placeOrder(java.lang.String):java.lang.String"
   * To: "placeOrder(String):String"
   */
  public static String normalizeSignature(String signature) {
    if (signature == null || signature.isEmpty()) {
      return signature;
    }

    // Replace fully-qualified type names with simple type names
    String normalized = FQN_PATTERN.matcher(signature).replaceAll("$1");

    return normalized;
  }

  /**
   * Create a normalized method ID.
   * Format: "<fully.qualified.ClassName>#<normalizedMethodName>"
   * Example: "com.greens.order.core.OrderService#placeOrder(String):String"
   */
  public static String createNormalizedMethodId(String className, String methodName, String signature) {
    if (className == null || methodName == null) {
      return null;
    }

    String normalizedSig = signature != null ? normalizeSignature(signature) : "";

    // Extract just the method name and parameters from signature if present
    // Format: methodName(Type1,Type2):ReturnType
    if (normalizedSig != null && !normalizedSig.isEmpty()) {
      // Check if signature already has the method name, if so just use it
      if (normalizedSig.startsWith(methodName)) {
        return className + "#" + normalizedSig;
      }
    }

    return className + "#" + methodName;
  }

  /**
   * Normalize an endpoint ID.
   * Format: "endpoint:<HTTP_METHOD> <PATH>"
   * Example: "endpoint:POST /api/orders/{id}"
   */
  public static String normalizeEndpointId(String httpMethod, String path) {
    if (httpMethod == null || path == null) {
      return null;
    }
    return "endpoint:" + httpMethod.toUpperCase() + " " + path;
  }

  /**
   * Normalize a topic ID.
   * Format: "topic:<topicName>"
   * Example: "topic:orders.v1"
   */
  public static String normalizeTopicId(String topicName) {
    if (topicName == null) {
      return null;
    }
    // Remove "topic:" prefix if already present
    String cleanName = topicName.startsWith("topic:") ? topicName.substring(6) : topicName;
    return "topic:" + cleanName;
  }

  /**
   * Normalize a class ID.
   * Format: "<fully.qualified.ClassName>"
   * Example: "com.greens.order.core.OrderService"
   */
  public static String normalizeClassId(String className) {
    return className; // Already in correct format
  }

  /**
   * Derive service name from module name or package.
   * Example: "order" or "order-service" from "com.greens.order.core"
   */
  public static String deriveServiceName(String moduleName, String packageName) {
    if (moduleName != null && !moduleName.isEmpty()) {
      return moduleName;
    }

    if (packageName != null && !packageName.isEmpty()) {
      // Extract first meaningful part: "com.greens.order" -> "order"
      String[] parts = packageName.split("\\.");
      if (parts.length > 0) {
        // Get the last non-empty part (likely the service/module name)
        for (int i = parts.length - 1; i >= 0; i--) {
          if (!parts[i].isEmpty() && !parts[i].equals("com") && !parts[i].equals("greens")) {
            return parts[i];
          }
        }
      }
    }

    return "default-service";
  }
}

