package com.github.sem2mqtt.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.sem2mqtt.SemToMqttAppException;
import com.github.sem2mqtt.configuration.MqttConfig;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

class MqttConnectionTest {

  public static final String MQTT_PASSWORD = "password";
  public static final String MQTT_USERNAME = "user";
  public static final String MQTT_CLIENT_ID = "mqtt-client-id";
  private final MqttClient mqttClientMock = Mockito.mock(MqttClient.class);
  private MqttConnection mqttConnection;

  @BeforeEach
  void setUp() {
    mqttConnection = new MqttConnection(
        mqttClientMock,
        new MqttConfig("rootTopic", "tcp://some-url", MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD));
  }

  @Test
  void connects_to_mqtt_server_when_establishing_connection() throws MqttException {
    //when
    mqttConnection.establish();
    //then
    verify(mqttClientMock).connect(argThat(mqttConnectOptions -> {
      assertThat(mqttConnectOptions.getUserName()).isEqualTo(MQTT_USERNAME);
      assertThat(mqttConnectOptions.getPassword()).isEqualTo(MQTT_PASSWORD.toCharArray());
      return true;
    }));
  }

  @Test
  void enables_auto_reconnect_when_establishing_connection() throws MqttException {
    //when
    mqttConnection.establish();
    //then
    verify(mqttClientMock).connect(argThat(MqttConnectOptions::isAutomaticReconnect));
  }

  @Test
  void crashes_app_when_security_exception_occurs_while_establishing() throws MqttException {
    //when
    doThrow(MqttSecurityException.class).when(mqttClientMock).connect(any(MqttConnectOptions.class));
    //then
    assertThatCode(() -> mqttConnection.establish()).isInstanceOf(SemToMqttAppException.class)
        .hasMessageContaining("mqtt")
        .hasMessageContaining("connect")
        .hasMessageContaining("not authorized");
  }

  @Test
  void crashes_app_when_cannot_connect_to_mqtt_server() throws MqttException {
    //when
    doThrow(MqttException.class).when(mqttClientMock).connect(any(MqttConnectOptions.class));
    //then
    assertThatCode(() -> mqttConnection.establish()).isInstanceOf(SemToMqttAppException.class)
        .hasMessageContaining("mqtt")
        .hasMessageContaining("connect");
  }

  @Test
  void subscribes_to_topics_when_requesting_subscription() throws MqttException {
    //given
    String topic = "measurements/plug2/+/set";
    //when
    mqttConnection.subscribe(topic, mock(MessageCallback.class));
    //then
    verify(mqttClientMock).subscribe(eq(topic), any(IMqttMessageListener.class));
  }

  @Test
  void crashes_app_when_cannot_subscribe_to_topics() throws MqttException {
    //when
    doThrow(MqttException.class).when(mqttClientMock).subscribe(anyString(), any(IMqttMessageListener.class));
    //then
    assertThatCode(() -> mqttConnection.subscribe("some/event", mock(MessageCallback.class)))
        .isInstanceOf(SemToMqttAppException.class)
        .hasMessageContaining("subscribe")
        .hasMessageContaining("mqtt")
        .hasMessageContaining("topic")
        .hasMessageContaining("some/event");
  }

  @Test
  void forwards_message_to_mqttclient_when_publishing_message() throws MqttException {
    //given
    mqttConnection.establish();
    //when
    mqttConnection.publish("some/topic", "Any kind of payload");
    //then
    verify(mqttClientMock).publish(eq("some/topic"), refEq("Any kind of object".getBytes(StandardCharsets.UTF_8)),
        anyInt(), anyBoolean());
  }

  @Test
  void invokes_callback_when_message_for_subscribed_topic_arrives() throws MqttException {
    //given
    String topic = "measurements/plug2/+/set";
    MessageCallback callback = mock(MessageCallback.class);
    //when
    mqttConnection.subscribe(topic, callback);
    //then
    verify(mqttClientMock).subscribe(eq(topic), argThat(whenCalledForwardsToCallback(callback)));
  }

  @Test
  void does_not_fail_on_null_when_loglevel_is_info() {
    //when
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = loggerContext.getLogger(MqttConnection.class);
    logger.setLevel(Level.INFO);
    //then
    assertThatCode(() -> mqttConnection.deliveryComplete(null))
        .doesNotThrowAnyException();
  }

  @Test
  void informs_about_connection_loss_when_mqtt_connection_is_lost() {
    //given
    Appender<ILoggingEvent> logAppenderMock = observeLogsOf(MqttConnection.class);
    //when
    mqttConnection.connectionLost(mock(Throwable.class));
    //then
    verify(logAppenderMock).doAppend(argThat(logs("connection", "lost")));
  }

  @Test
  void informs_about_delivery_completion_when_mqtt_notifies() {
    //given
    Appender<ILoggingEvent> logAppenderMock = observeLogsOf(MqttConnection.class);
    //when
    mqttConnection.deliveryComplete(mock(IMqttDeliveryToken.class, RETURNS_MOCKS));
    //then
    verify(logAppenderMock).doAppend(argThat(logs("message", "sent")));
  }

  @Test
  void informs_about_new_message_when_mqtt_message_arrived() {
    //given
    Appender<ILoggingEvent> logAppenderMock = observeLogsOf(MqttConnection.class);
    //when
    mqttConnection.messageArrived("some/topic", mock(MqttMessage.class));
    //then
    verify(logAppenderMock).doAppend(argThat(logs("received", "message", "some/topic")
    ));
  }

  private Appender<ILoggingEvent> observeLogsOf(Class<?> classToObserveLogsOf) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = loggerContext.getLogger(classToObserveLogsOf);
    Appender<ILoggingEvent> logAppenderMock = mock(Appender.class);
    logger.addAppender(logAppenderMock);
    return logAppenderMock;
  }

  private ArgumentMatcher<ILoggingEvent> logs(String... expectedStringsInLog) {
    return logArgument -> {
      AbstractStringAssert<?> abstractStringAssert = assertThat(logArgument)
          .asInstanceOf(InstanceOfAssertFactories.type(ILoggingEvent.class))
          .extracting(ILoggingEvent::getFormattedMessage).asString();
      for (String expectedMessage : expectedStringsInLog) {
        abstractStringAssert.containsIgnoringCase(expectedMessage);
      }
      return true;
    };
  }

  private ArgumentMatcher<IMqttMessageListener> whenCalledForwardsToCallback(MessageCallback callback) {
    return listener -> {
      String messageTopic = "measurements/plug2/led/set";
      MqttMessage message = randomMessage();
      try {
        listener.messageArrived(messageTopic, message);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      verify(callback).handleMqttMessage(messageTopic, message);
      return true;
    };
  }

  private MqttMessage randomMessage() {
    byte[] randomMessage = new byte[128];
    ThreadLocalRandom.current().nextBytes(randomMessage);
    return new MqttMessage(randomMessage);
  }

}