package com.github.sem2mqtt;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.Sem6000MqttTopic;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class BridgeMessageHandlingException extends Exception {

  public BridgeMessageHandlingException(String invalidTopic) {
    super(String.format("The mqtt topic '%s' is not valid.", invalidTopic));
  }

  public BridgeMessageHandlingException(MqttMessage message, Sem6000MqttTopic topic, Sem6000Config sem6000Config,
      SendingException e) {
    super(String.format("Failed to forward mqtt message '%s' to topic '%s' for '%s'.", message, topic,
        sem6000Config.getName()), e);
  }
}
