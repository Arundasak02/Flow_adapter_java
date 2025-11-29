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

public class ConfigLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

  private final Map<String, String> properties = new HashMap<>();

  public ConfigLoader(Path configPath) {
    if (isValidConfigPath(configPath)) {
      loadAllProperties(configPath);
    } else {
      logger.warn("Config path is null or not a directory: {}", configPath);
    }
  }

  private boolean isValidConfigPath(Path path) {
    return path != null && Files.isDirectory(path);
  }

  private void loadAllProperties(Path configPath) {
    try (java.util.stream.Stream<Path> stream = Files.walk(configPath)) {
      stream.filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".properties"))
            .forEach(this::loadPropertiesFile);
      logger.info("Loaded {} properties from {}", properties.size(), configPath);
    } catch (IOException e) {
      logger.warn("Error loading configuration from {}", configPath, e);
    }
  }

  private void loadPropertiesFile(Path file) {
    try (InputStream is = Files.newInputStream(file)) {
      Properties props = new Properties();
      props.load(is);
      addPropertiesToMap(props);
      logger.info("Loaded {} properties from {}", props.size(), file);
    } catch (IOException e) {
      logger.warn("Error loading properties file {}", file, e);
    }
  }

  private void addPropertiesToMap(Properties props) {
    props.forEach((key, value) -> {
      String keyStr = key.toString();
      String valStr = value.toString();
      properties.put(keyStr, valStr);
      logger.debug("Loaded property: {} = {}", keyStr, valStr);
    });
  }

  public String resolvePlaceholders(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }

    String resolved = replacePlaceholders(raw);
    return handleUnresolvedPlaceholder(raw, resolved);
  }

  private String replacePlaceholders(String raw) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
    StringBuilder sb = new StringBuilder(raw.length());
    int lastIndex = 0;

    while (matcher.find()) {
      sb.append(raw, lastIndex, matcher.start());
      String placeholder = resolveSinglePlaceholder(matcher);
      sb.append(placeholder);
      lastIndex = matcher.end();
    }

    sb.append(raw, lastIndex, raw.length());
    return sb.toString();
  }

  private String resolveSinglePlaceholder(Matcher matcher) {
    String key = matcher.group(1);
    String value = properties.get(key);
    return value != null ? value : matcher.group(0);
  }

  private String handleUnresolvedPlaceholder(String original, String resolved) {
    if (original.equals(resolved) && original.matches(PLACEHOLDER_PATTERN.pattern())) {
      Matcher matcher = PLACEHOLDER_PATTERN.matcher(original);
      if (matcher.find()) {
        String key = matcher.group(1);
        if (!properties.containsKey(key)) {
          return "";
        }
      }
    }
    return resolved;
  }
}