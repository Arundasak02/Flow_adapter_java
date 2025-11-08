package com.flow.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class ConfigLoader {

  private final Map<String, String> properties = new HashMap<>();
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  public ConfigLoader(Path configPath) {
    if (configPath != null && Files.isDirectory(configPath)) {
      try {
        Files.walk(configPath)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".properties"))
            .forEach(this::loadPropertiesFile);
      } catch (IOException e) {
        System.err.println("Error loading configuration from " + configPath + ": " + e.getMessage());
      }
    }
  }

  private void loadPropertiesFile(Path file) {
    Properties props = new Properties();
    try (InputStream is = Files.newInputStream(file)) {
      props.load(is);
      props.forEach((key, value) -> properties.put(key.toString(), value.toString()));
    } catch (IOException e) {
      System.err.println("Error loading properties file " + file + ": " + e.getMessage());
    }
  }

  public String resolvePlaceholders(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
    StringBuffer sb = new StringBuffer();
    boolean foundAndReplaced = false;

    while (matcher.find()) {
      String key = matcher.group(1);
      String value = properties.get(key);
      if (value != null) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        foundAndReplaced = true;
      } else {
        // If a placeholder is found but not resolved, keep the original placeholder text.
        // This ensures that if only part of a string is a placeholder, the rest remains.
        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
      }
    }
    matcher.appendTail(sb);

    // Special handling for the case where the entire raw string was a single placeholder
    // and it was not resolved, as per the problem description ("returns an empty string").
    if (!foundAndReplaced && raw.matches(PLACEHOLDER_PATTERN.pattern())) {
        String key = PLACEHOLDER_PATTERN.matcher(raw).group(1);
        if (!properties.containsKey(key)) {
            return ""; // Return empty string if the entire raw string was an unresolved placeholder
        }
    }

    return sb.toString();
  }
}