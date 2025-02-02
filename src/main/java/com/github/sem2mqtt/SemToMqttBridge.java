package com.github.sem2mqtt;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.bluetooth.sem6000.SendingException;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import com.github.sem2mqtt.mqtt.Sem6000MqttTopic;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.sem6000.connector.receive.AvailabilityResponse;
import org.magcode.sem6000.connector.receive.AvailabilityResponse.Availability;
import org.magcode.sem6000.connector.receive.DataDayResponse;
import org.magcode.sem6000.connector.receive.MeasurementResponse;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.send.LedCommand;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.magcode.sem6000.connector.send.SwitchCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemToMqttBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemToMqttBridge.class);

  private final MqttConnection mqttConnection;
  private final Set<Sem6000Config> sem6000Configs;
  private final String rootTopic;
  private final BluetoothConnectionManager bluetoothConnectionManager;
  private final Scheduler scheduler;
  private ZonedDateTime lastNotifiedAboutOnlineAvailabilityAt;

  public SemToMqttBridge(String rootTopic, Set<Sem6000Config> sem6000Configs, MqttConnection mqttConnection,
      BluetoothConnectionManager bluetoothConnectionManager, Scheduler scheduler) {

    this.mqttConnection = mqttConnection;
    this.sem6000Configs = sem6000Configs;
    this.rootTopic = rootTopic;
    this.bluetoothConnectionManager = bluetoothConnectionManager;
    this.scheduler = scheduler;
  }

  public void run() {
    LOGGER.info("Starting bridge service.");
    mqttConnection.establish();
    bluetoothConnectionManager.init();
    sem6000Configs.forEach(sem6000Config -> {
      Sem6000Connection sem6000Connection = bluetoothConnectionManager.setupConnection(
          new Sem6000Connection(sem6000Config, bluetoothConnectionManager, scheduler));
      sem6000Connection.establish();
      sem6000Connection.subscribe(semResponse -> this.handleSem6000Response(semResponse, sem6000Config));
      subscribeToSem6000MqttTopics(sem6000Config, sem6000Connection);
    });
  }


  void subscribeToSem6000MqttTopics(Sem6000Config sem6000Config, Sem6000Connection sem6000Connection) {
    mqttConnection.subscribe(Sem6000MqttTopic.createSetterSubscription(rootTopic, sem6000Config.getName()),
        createMessageCallbackFor(sem6000Config, sem6000Connection));
  }

  private MessageCallback createMessageCallbackFor(Sem6000Config sem6000Config, Sem6000Connection sem6000Connection) {
    return (String topic, MqttMessage message) -> {
      try {
        handleMqttMessage(
            new Sem6000MqttTopic(rootTopic, topic, sem6000Config.getName()), message, sem6000Config, sem6000Connection);
      } catch (BridgeMessageHandlingException e) {
        LOGGER.atDebug().log("Failed to process mqtt message '{}' to topic '{}' for device '{}': {}", topic,
            message, sem6000Config.getName(), e.getMessage());
      }
    };
  }

  void handleMqttMessage(Sem6000MqttTopic topic, MqttMessage message, Sem6000Config sem6000Config,
      Sem6000Connection sem6000Connection) throws BridgeMessageHandlingException {
    LOGGER.atDebug()
        .log("Received mqtt message '{}' to topic {} for device {}", message, topic, sem6000Config.getName());
    if (!topic.isValid()) {
      throw new BridgeMessageHandlingException(topic.toString());
    }

    switch (topic.getType()) {
      case RELAY:
        sendRelaySwitchCommandToSem6000(topic, message, sem6000Config, sem6000Connection);
        break;
      case LED:
        sendLedSwitchCommandToSem6000(topic, message, sem6000Config, sem6000Connection);
        break;
      default:
        LOGGER.warn("Ignoring mqtt message '{}' to topic '{}' for device '{}' and unknown type {}", message, topic,
            sem6000Config.getName(), topic.getType());
        break;
    }
  }

  private void sendLedSwitchCommandToSem6000(Sem6000MqttTopic topic, MqttMessage message, Sem6000Config sem6000Config,
      Sem6000Connection sem6000Connection) throws BridgeMessageHandlingException {
    boolean ledOnOff = Boolean.parseBoolean(message.toString());
    try {
      sem6000Connection.safeSend(new LedCommand(ledOnOff));
      LOGGER.info("Forwarded 'switch led {}' to {}", sem6000Config.getName(), ledOnOff ? "on" : "off");
    } catch (SendingException e) {
      throw new BridgeMessageHandlingException(message, topic, sem6000Config, e);
    }
  }

  private void sendRelaySwitchCommandToSem6000(Sem6000MqttTopic topic, MqttMessage message, Sem6000Config sem6000Config,
      Sem6000Connection sem6000Connection) throws BridgeMessageHandlingException {
    boolean plugOnOff = Boolean.parseBoolean(message.toString());
    try {
      sem6000Connection.safeSend(new SwitchCommand(plugOnOff));
      // request measurement as switching probably influences the plugs consumption
      sem6000Connection.safeSend(new MeasureCommand());
      LOGGER.info("Forwarded 'switch relay {}' to {}", sem6000Config.getName(), plugOnOff ? "on" : "off");
    } catch (SendingException e) {
      throw new BridgeMessageHandlingException(message, topic, sem6000Config, e);
    }
  }

  private synchronized void handleSem6000Response(SemResponse response, Sem6000Config sem6000Config) {
    switch (response.getType()) {
      case MEASURE:
        MeasurementResponse mr = (MeasurementResponse) response;
        LOGGER.info("Forwarding sem6000 measurement '{}' to mqtt for device '{}'", mr, sem6000Config.getName());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/voltage", mr.getVoltage());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/power", mr.getPower());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/relay", mr.isPowerOn());
        break;
      case DATADAY:
        DataDayResponse dr = (DataDayResponse) response;
        LOGGER.info("Forwarding daily data response '{}' to mqtt for device '{}'", dr, sem6000Config.getName());
        mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/energytoday", dr.getToday());
        break;
      case AVAILABILITY:
        AvailabilityResponse ar = (AvailabilityResponse) response;
        // avoid notifying about online state too often
        if (ar.getAvailability() != Availability.AVAILABLE || Objects.isNull(lastNotifiedAboutOnlineAvailabilityAt)
            || now().isAfter(
            lastNotifiedAboutOnlineAvailabilityAt.plus(sem6000Config.getUpdateInterval().toSeconds(), SECONDS))) {
          LOGGER.info("Forwarding sem6000 availability '{}' to mqtt for device '{}'", ar, sem6000Config.getName());
          String payload = ar.getAvailability() == Availability.AVAILABLE ? "online" : "lost";
          mqttConnection.publish(rootTopic + "/" + sem6000Config.getName() + "/state", payload);
          // when notifying about online state, update last notification time
          lastNotifiedAboutOnlineAvailabilityAt =
              ar.getAvailability() == Availability.AVAILABLE ? now() : lastNotifiedAboutOnlineAvailabilityAt;
        }
        break;
      default:
        break;
    }
  }
}
