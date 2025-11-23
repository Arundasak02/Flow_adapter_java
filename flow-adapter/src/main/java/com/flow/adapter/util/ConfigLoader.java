package com.flow.adapter.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads simple properties files from a config directory and allows resolving ${placeholders}
 * against the loaded properties. This class intentionally keeps behavior minimal (only
 * .properties files) to avoid heavy YAML parsing dependencies in the adapter core.
 */
public class ConfigLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

  private final Map<String, String> properties = new HashMap<>();
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  public ConfigLoader(Path configPath) {
    if (configPath != null && Files.isDirectory(configPath)) {
      try (java.util.stream.Stream<Path> stream = Files.walk(configPath)) {
        stream
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".properties"))
            .forEach(this::loadPropertiesFile);
      } catch (IOException e) {
        logger.warn("Error loading configuration from {}", configPath, e);
      }
    }
  }

  private void loadPropertiesFile(Path file) {
    Properties props = new Properties();
    try (InputStream is = Files.newInputStream(file)) {
      props.load(is);
      props.forEach((key, value) -> properties.put(key.toString(), value.toString()));
    } catch (IOException e) {
      logger.warn("Error loading properties file {}", file, e);
    }
  }

  /**
   * Resolve placeholders of the form ${key} using loaded properties.
   * If a placeholder cannot be resolved, the original placeholder text is preserved.
   * If the whole input is a single unresolved placeholder, returns the empty string.
   */
  public String resolvePlaceholders(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
    StringBuilder sb = new StringBuilder(raw.length());
    int last = 0;
    boolean foundAndReplaced = false;
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      sb.append(raw, last, start);
      String key = matcher.group(1);
      String value = properties.get(key);
      if (value != null) {
        sb.append(value);
        foundAndReplaced = true;
      } else {
        // Keep the original placeholder text when unresolved
        sb.append(matcher.group(0));
      }
      last = end;
    }
    // append remainder
    if (last < raw.length()) {
      sb.append(raw, last, raw.length());
    }

    if (!foundAndReplaced && raw.matches(PLACEHOLDER_PATTERN.pattern())) {
      String key = PLACEHOLDER_PATTERN.matcher(raw).group(1);
      if (!properties.containsKey(key)) {
        return "";
      }
    }

    return sb.toString();
  }
}