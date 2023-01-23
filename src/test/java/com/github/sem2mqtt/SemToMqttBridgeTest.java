package com.github.sem2mqtt;

import static com.github.sem2mqtt.ObservingLogTestHelper.atLeastOneMatches;
import static com.github.sem2mqtt.ObservingLogTestHelper.logs;
import static com.github.sem2mqtt.ObservingLogTestHelper.observeLogsOf;
import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.generateSemConfigs;
import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.randomSemConfigForPlug;
import static com.github.sem2mqtt.mqtt.Sem6000MqttTopic.Type.UNKNOWN;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.magcode.sem6000.connector.receive.AvailabilityResponse.Availability.AVAILABLE;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createMeasureResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemDayDataResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createUnknownSemResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000Connection;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusHandlerProxy.Sem6000ResponseHandler;
import com.github.sem2mqtt.bluetooth.sem6000.SendingException;
import com.github.sem2mqtt.configuration.Sem6000Config;
import com.github.sem2mqtt.mqtt.MqttConnection;
import com.github.sem2mqtt.mqtt.MqttConnection.MessageCallback;
import com.github.sem2mqtt.mqtt.Sem6000MqttTopic;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.magcode.sem6000.connector.receive.AvailabilityResponse;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.send.Command;
import org.magcode.sem6000.connector.send.LedCommand;
import org.magcode.sem6000.connector.send.SwitchCommand;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class SemToMqttBridgeTest {

  public static final String ROOT_TOPIC = "rootTopic";
  public Scheduler scheduler = new Scheduler();
  private MqttConnection mqttConnectionMock;
  private BluetoothConnectionManager bluetoothConnectionManager;
  private Sem6000Connection defaultSem6000ConnectionMock;
  @Captor
  private ArgumentCaptor<Sem6000ResponseHandler> semResponseHandlerCaptor;

  @BeforeEach
  void setUp() {
    mqttConnectionMock = mock(MqttConnection.class);
    bluetoothConnectionManager = mock(BluetoothConnectionManager.class);

    defaultSem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(defaultSem6000ConnectionMock);
  }

  @AfterEach
  void tearDown() {
    scheduler.gracefullyShutdown();
  }

  @Test
  void establishes_mqtt_connection_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, generateSemConfigs(1),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    //when
    semToMqttBridge.run();
    //then
    verify(mqttConnectionMock).establish();
  }

  @Test
  void subscribes_to_mqtt_setter_of_each_sem6000_when_running() {
    //given
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(4);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, scheduler);
    //when
    semToMqttBridge.run();
    //then
    for (Sem6000Config sem6000Config : sem6000Configs) {
      verify(mqttConnectionMock).subscribe(
          matches(String.format("^%s\\/%s\\/\\+\\/set$", ROOT_TOPIC, sem6000Config.getName())),
          any(MessageCallback.class));
    }
  }

  @ParameterizedTest
  @MethodSource("mqttMessages")
  void forwards_mqtt_messages_to_sem6000_device_when_running(String message, String type, Command expectedCommand)
      throws SendingException, BridgeMessageHandlingException {
    //given
    String plugName = "plug1";
    Sem6000Config plug = randomSemConfigForPlug(plugName);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, singleton(plug),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    semToMqttBridge.run();
    //when
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    semToMqttBridge.handleMqttMessage(
        new Sem6000MqttTopic(ROOT_TOPIC, String.format("%s/%s/%s", ROOT_TOPIC, plugName, type), plugName),
        new MqttMessage(message.getBytes(StandardCharsets.UTF_8)), plug, sem6000ConnectionMock);
    //then
    verify(sem6000ConnectionMock).safeSend(refEq(expectedCommand));
  }

  static Stream<Arguments> mqttMessages() throws SendingException {
    return Stream.of(Arguments.of("true", "led", new LedCommand(true)),
        Arguments.of("false", "led", new LedCommand(false)),
        Arguments.of("true", "relay", new SwitchCommand(true)),
        Arguments.of("false", "relay", new SwitchCommand(false)));
  }

  @Test
  void logs_when_message_is_not_processable() throws BridgeMessageHandlingException {
    //given
    String plugName = "plug1";
    Sem6000Config plug = randomSemConfigForPlug(plugName);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, singleton(plug),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    semToMqttBridge.run();
    Sem6000MqttTopic sem6000MqttTopic = mock(Sem6000MqttTopic.class);
    when(sem6000MqttTopic.isValid()).thenReturn(true);
    when(sem6000MqttTopic.getType()).thenReturn(UNKNOWN);
    Appender<ILoggingEvent> logObserver = observeLogsOf(SemToMqttBridge.class);
    //when
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    semToMqttBridge.handleMqttMessage(
        sem6000MqttTopic,
        new MqttMessage("some message".getBytes(StandardCharsets.UTF_8)), plug, sem6000ConnectionMock);
    //then
    verify(logObserver, atLeastOneMatches()).doAppend(argThat(logs("ignoring", "unknown", "message")));
  }

  @Test
  void establishes_a_bluetooth_connection_to_each_sem6000_when_running() {
    //given
    int countOfSem6000 = 4;
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(countOfSem6000);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, scheduler);
    //when
    semToMqttBridge.run();
    //then
    for (Sem6000Config sem6000Config : sem6000Configs) {
      verify(bluetoothConnectionManager).setupConnection(
          argThat(bluetoothConnection -> bluetoothConnection.getMacAddress().equals(sem6000Config.getMac())));
    }
    verify(defaultSem6000ConnectionMock, times(countOfSem6000)).establish();
  }

  @Test
  @MockitoSettings(strictness = Strictness.LENIENT)
  void fails_on_mqtt_problems_when_running() {
    //given
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, generateSemConfigs(1),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    //when
    doThrow(new RuntimeException()).when(mqttConnectionMock).establish();
    //then
    assertThatCode(semToMqttBridge::run).isInstanceOf(RuntimeException.class);
  }

  @Test
  void subscribes_to_sem6000_messages_when_running() {
    //given
    int countOfSem6000 = 4;
    Set<Sem6000Config> sem6000Configs = generateSemConfigs(countOfSem6000);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, sem6000Configs, mqttConnectionMock,
        bluetoothConnectionManager, scheduler);
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(sem6000ConnectionMock);
    //when
    semToMqttBridge.run();
    //then
    verify(sem6000ConnectionMock, times(countOfSem6000)).subscribe(any());
  }

  @ParameterizedTest
  @MethodSource("sem6000Messages")
  void forwards_sem6000_messages_according_to_message_type_when_retrieving_message(SemResponse semResponse,
      int expectedCountOfMessages) {
    //given
    String plugName = "plug1";
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, Set.of(randomSemConfigForPlug(plugName)),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    when(bluetoothConnectionManager.setupConnection(any())).thenReturn(sem6000ConnectionMock);
    //when
    semToMqttBridge.run();
    verify(sem6000ConnectionMock).subscribe(semResponseHandlerCaptor.capture());
    semResponseHandlerCaptor.getValue().handleSem6000Response(semResponse);
    //then
    verify(mqttConnectionMock, times(expectedCountOfMessages)).publish(
        startsWith(String.format("%s/%s/", ROOT_TOPIC, plugName)), any());
  }

  static Stream<Arguments> sem6000Messages() {
    return Stream.of(Arguments.of(createMeasureResponse(), 3), Arguments.of(createSemDayDataResponse(), 1),
        Arguments.of(new AvailabilityResponse(AVAILABLE), 1), Arguments.of(createUnknownSemResponse(), 0));
  }

  @Captor
  ArgumentCaptor<MessageCallback> messageCallbackCaptor;

  @Test
  void fails_gracefully_when_topic_does_not_match() {
    //given
    String plugName = "plug1";
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, singleton(randomSemConfigForPlug(plugName)),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    semToMqttBridge.run();
    verify(mqttConnectionMock).subscribe(anyString(), messageCallbackCaptor.capture());
    String invalidTopic = String.format("%s/%s/%s", ROOT_TOPIC, "wrong_plug_name", "led");
    Appender<ILoggingEvent> logObserver = observeLogsOf(SemToMqttBridge.class);
    //when
    messageCallbackCaptor.getValue()
        .handleMqttMessage(invalidTopic, new MqttMessage("any message".getBytes(StandardCharsets.UTF_8)));
    //then
    verify(logObserver, atLeastOneMatches()).doAppend(argThat(logs("failed", "process", "message", invalidTopic)));
  }

  @Test
  void fails_forward_when_led_command_fails() throws SendingException {
    //given
    String plugName = "plug1";
    Sem6000Config sem6000Config = randomSemConfigForPlug(plugName);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, singleton(sem6000Config),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    semToMqttBridge.run();
    //when
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    doThrow(SendingException.class).when(sem6000ConnectionMock).safeSend(any(LedCommand.class));
    //then
    assertThatCode(() -> semToMqttBridge.handleMqttMessage(
        new Sem6000MqttTopic(ROOT_TOPIC, String.format("%s/%s/%s", ROOT_TOPIC, plugName, "led"), plugName),
        new MqttMessage("on".getBytes(StandardCharsets.UTF_8)), sem6000Config, sem6000ConnectionMock)).isInstanceOf(
        BridgeMessageHandlingException.class).hasMessageContainingAll("Failed", "forward", plugName);
  }

  @Test
  void fails_forward_when_relay_command_fails() throws SendingException {
    //given
    String plugName = "plug1";
    Sem6000Config sem6000Config = randomSemConfigForPlug(plugName);
    SemToMqttBridge semToMqttBridge = new SemToMqttBridge(ROOT_TOPIC, singleton(sem6000Config),
        mqttConnectionMock, bluetoothConnectionManager, scheduler);
    semToMqttBridge.run();
    //when
    Sem6000Connection sem6000ConnectionMock = mock(Sem6000Connection.class);
    doThrow(SendingException.class).when(sem6000ConnectionMock).safeSend(any(SwitchCommand.class));
    //then
    assertThatCode(() -> semToMqttBridge.handleMqttMessage(
        new Sem6000MqttTopic(ROOT_TOPIC, String.format("%s/%s/%s", ROOT_TOPIC, plugName, "relay"), plugName),
        new MqttMessage("on".getBytes(StandardCharsets.UTF_8)), sem6000Config, sem6000ConnectionMock)).isInstanceOf(
        BridgeMessageHandlingException.class).hasMessageContainingAll("Failed", "forward", plugName);
  }
}