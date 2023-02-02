package com.github.sem2mqtt.configuration;

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sem2mqtt.SemToMqttAppException;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);
  public static final String DEFAULT_PROPERTY_FILENAME = "sem6.properties";
  public static final String DEFAULT_YAML_FILENAME = "sem2mqtt_bridge.yaml";

  private final ObjectMapper yamlMapper;

  public ConfigurationLoader(ObjectMapper yamlMapper) {
    this.yamlMapper = yamlMapper;
  }

  public Configuration load(String configurationFileName) {
    File file = getClassPathFileFor(configurationFileName);
    failIfFileDoesNotExist(file);
    if (configurationFileName.endsWith(".yaml")) {
      return loadFromYaml(file);
    } else if (configurationFileName.endsWith(".properties")) {
      return loadFromProperties(file);
    }

    throw new IllegalArgumentException("Unknown file type.");
  }

  private void failIfFileDoesNotExist(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException(String.format(
          "Configuration file '%s' does not exist.", file.getAbsoluteFile()));
    }
  }

  public Configuration load() {
    LOGGER.debug("Scanning for configuration file.");
    File propertiesFile = getClassPathFileFor("./" + DEFAULT_PROPERTY_FILENAME);
    if (propertiesFile.exists()) {
      LOGGER.debug("Found '" + DEFAULT_PROPERTY_FILENAME + "'.");
      return loadFromProperties(propertiesFile);
    }

    File yamlFile = getClassPathFileFor("./" + DEFAULT_YAML_FILENAME);
    if (yamlFile.exists()) {
      LOGGER.debug("Found '" + DEFAULT_YAML_FILENAME + "'.");
      return loadFromYaml(yamlFile);
    }

    LOGGER.error("No configuration file found.");
    throw new IllegalArgumentException(
        "You need to offer configuration in either '" + DEFAULT_PROPERTY_FILENAME + "' or '" + DEFAULT_YAML_FILENAME
            + "'.");
  }

  private Configuration loadFromProperties(File propertiesFile) {
    LOGGER.atInfo().log(() -> String.format("Loading config from '%s'", propertiesFile.getName()));
    InputStream inputStream = null;
    try {
      Properties props = new Properties();
      inputStream = Files.newInputStream(propertiesFile.toPath());
      props.load(inputStream);
      MqttConfig mqttConfig = new MqttConfig(props.getProperty("rootTopic"),
          props.getProperty("mqttServer"), props.getProperty("mqttClientId"),
          props.getProperty("mqttUsername"), props.getProperty("mqttPassword"));
      Set<Sem6000Config> semConfigs = new HashSet<>();
      for (int i = 1; i < 11; i++) {
        if (props.containsKey("sem" + i + ".mac")) {
          semConfigs.add(createConfigFromProperties(props, i));
        }
      }

      LOGGER.info("Successfully loaded properties config.");
      return new Configuration(mqttConfig, semConfigs, emptySet());
    } catch (IOException e) {
      failOnReadError(e, propertiesFile.getName());
      throw new SemToMqttAppException("Unable to read properties file", e);
    } finally {
      safelyCloseFileInputStream(inputStream);
    }
  }

  private Sem6000Config createConfigFromProperties(Properties props, int i) {
    return new Sem6000Config(props.getProperty("sem" + i + ".mac"),
        props.getProperty("sem" + i + ".pin"),
        props.getProperty("sem" + i + ".name"),
        Optional.ofNullable(props.getProperty("sem" + i + ".refresh"))
            .map(Integer::valueOf).map(
                Duration::ofSeconds).orElse(null));
  }

  private void safelyCloseFileInputStream(InputStream inputStream) {
    try {
      if (inputStream != null) {
        inputStream.close();
      }
    } catch (IOException e) {
      LOGGER.debug("Failed to close properties file.", e);
    }
  }

  private Configuration loadFromYaml(File yamlFile) {
    LOGGER.atInfo().log(() -> String.format("Loading config from '%s'", yamlFile.getName()));
    try {
      Configuration configuration = yamlMapper.readValue(yamlFile, Configuration.class);
      LOGGER.info("Successfully loaded yaml config.");
      return configuration;
    } catch (IOException e) {
      failOnReadError(e, yamlFile.getName());
      throw new SemToMqttAppException("Unable to read yaml file", e);
    }
  }

  private void failOnReadError(Exception e, String fileName) {
    LOGGER.atError().log(() -> String.format("Failed to load configuration file '%s': %s.", fileName, e.getMessage()));
  }

  private File getClassPathFileFor(String filePath) {
    URL resource = this.getClass().getClassLoader().getResource(filePath);
    return Optional.ofNullable(resource).map(URL::getFile).map(File::new).orElse(new File(filePath));
  }
}
