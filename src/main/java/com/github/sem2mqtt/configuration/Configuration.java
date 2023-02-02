package com.github.sem2mqtt.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Config;
import com.github.sem2mqtt.bluetooth.timeflip.TimeFlipConfiguration;
import java.util.Optional;
import java.util.Set;

public class Configuration {

  private final MqttConfig mqttConfig;
  private final Set<Sem6000Config> semConfigs;

  private final Set<TimeFlipConfiguration> timeFlipConfigs;

  public Configuration(@JsonProperty(value = "mqtt", defaultValue = "") MqttConfig mqttConfig,
      @JsonProperty(value = "sem", defaultValue = "") Set<Sem6000Config> semConfigs,
      @JsonProperty(value = "timeflip") Set<TimeFlipConfiguration> timeFlipConfigs) {
    this.mqttConfig = Optional.ofNullable(mqttConfig).orElse(MqttConfig.defaults());
    this.semConfigs = semConfigs;
    this.timeFlipConfigs = timeFlipConfigs;
  }

  public MqttConfig getMqttConfig() {
    return mqttConfig;
  }

  public Set<Sem6000Config> getSemConfigs() {
    return semConfigs;
  }

  public Set<TimeFlipConfiguration> getTimeFlipConfigs() {
    return timeFlipConfigs;
  }
}
