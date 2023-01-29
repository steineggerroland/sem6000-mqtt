package com.github.sem2mqtt.bluetooth.sem6000;

import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.DBUS_PATH_01;
import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.createMeasurementPropertyChange;
import static com.github.sem2mqtt.configuration.Sem6000ConfigTestHelper.randomSemConfigForPlug;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coreoz.wisp.Scheduler;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusHandlerProxy.Sem6000ResponseHandler;
import com.github.sem2mqtt.configuration.Sem6000Config;
import java.time.Duration;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.magcode.sem6000.connector.send.MeasureCommand;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class Sem6000ConnectionTest {

  private final Scheduler scheduler = new Scheduler();
  private BluetoothDevice sem6000DeviceMock;
  private BluetoothGattCharacteristic writeService;
  private BluetoothGattCharacteristic notifyService;
  private BluetoothConnectionManager bluetoothConnectionManagerMock;
  @Captor
  ArgumentCaptor<DbusListener> dbusListenerCaptor;

  /**
   * Set up a connection manager with a device that is connected and returns sem6000 gatt service and its write and
   * notify services.
   */
  @BeforeEach
  void setUp() throws Exception {
    bluetoothConnectionManagerMock = mock(BluetoothConnectionManager.class);
    sem6000DeviceMock = mock(BluetoothDevice.class, RETURNS_MOCKS);
    when(sem6000DeviceMock.connect()).thenReturn(true);
    when(sem6000DeviceMock.isConnected()).thenReturn(true);
    when(bluetoothConnectionManagerMock.findDeviceOrFail(anyString(), any())).thenReturn(sem6000DeviceMock);
    BluetoothGattService gattService = mock(BluetoothGattService.class, RETURNS_MOCKS);
    when(sem6000DeviceMock.getGattServiceByUuid(Sem6000GattCharacteristic.SERVICE.uuid)).thenReturn(gattService);
    writeService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.WRITE.uuid)).thenReturn(writeService);
    notifyService = mock(BluetoothGattCharacteristic.class, RETURNS_MOCKS);
    when(gattService.getGattCharacteristicByUuid(Sem6000GattCharacteristic.NOTIFY.uuid)).thenReturn(notifyService);
  }

  @AfterEach
  void tearDown() {
    scheduler.gracefullyShutdown();
  }

  @Test
  @MockitoSettings(strictness = Strictness.LENIENT)
  void mac_matches_from_config_when_creating_connection() {
    //given
    Sem6000Config sem6000Config = randomSemConfigForPlug("plug1");
    //when
    Sem6000Connection sem6000Connection = new Sem6000Connection(sem6000Config,
        new BluetoothConnectionManager(mock(DeviceManager.class)), scheduler);
    //then
    assertThat(sem6000Connection.getMacAddress()).isEqualTo(sem6000Connection.getMacAddress());
  }

  @Test
  void connects_to_bluetooth_device_when_establishing_connection() {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    verify(sem6000DeviceMock).connect();
  }

  @Test
  void sends_login_and_sync_time_command_when_establishing_connection()
      throws BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    verify(writeService, times(2)).writeValue(any(), anyMap());
  }

  @Test
  void connection_is_established_when_establishing_connection() {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    //when
    sem6000Connection.establish();
    //then
    assertThat(sem6000Connection.isEstablished()).isTrue();
  }

  @Test
  void sends_measurement_and_day_requests_when_connection_is_established()
      throws InterruptedException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Duration updateInterval = Duration.ofMillis(20);
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", updateInterval),
        bluetoothConnectionManagerMock, scheduler);
    //when
    sem6000Connection.establish();
    reset(writeService); // reset to ignore e.g. the login message
    // wait longer than update interval, because of the message processing overhead
    final int COUNT_OF_MEASUREMENT_MESSAGES_PER_REQUEST = 2;
    await().atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () -> verify(writeService, atLeast(COUNT_OF_MEASUREMENT_MESSAGES_PER_REQUEST))
                .writeValue(any(), anyMap()));
  }

  @Test
  void sends_measurement_requests_regularly_when_connection_is_established()
      throws InterruptedException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Duration updateInterval = Duration.ofMillis(15);
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", updateInterval),
        bluetoothConnectionManagerMock, scheduler);
    //when
    sem6000Connection.establish();
    reset(writeService); // reset to ignore e.g. the login message
    // there is an overhead when processing the message, therefore, we wait twice the update interval
    await().atLeast(Duration.ofMillis(updateInterval.toMillis() * 10))
        .pollDelay(Duration.ofMillis(updateInterval.toMillis()))
        .untilAsserted(() -> {
              final int COUNT_OF_MEASUREMENT_MESSAGES_PER_REQUEST = 2;
              verify(writeService, atLeast(COUNT_OF_MEASUREMENT_MESSAGES_PER_REQUEST * 10))
                  .writeValue(any(), anyMap());
            }
        );
  }

  @Test
  void forwards_bluetooth_messages_when_subscribed_for_messages()
      throws DBusException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1", Duration.ofSeconds(60)),
        bluetoothConnectionManagerMock, scheduler);
    when(notifyService.getDbusPath()).thenReturn(DBUS_PATH_01);
    sem6000Connection.establish();
    Sem6000ResponseHandler responseHandler = mock(Sem6000ResponseHandler.class);
    verify(bluetoothConnectionManagerMock).subscribeToDbusPath(eq(DBUS_PATH_01), dbusListenerCaptor.capture());
    //when
    sem6000Connection.subscribe(responseHandler);
    dbusListenerCaptor.getValue().handle(createMeasurementPropertyChange(DBUS_PATH_01));
    //then
    verify(responseHandler, atLeastOnce()).handleSem6000Response(any());
  }

  @Test
  void resets_state_when_message_cannot_be_sent()
      throws SendingException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    sem6000Connection.establish();
    MeasureCommand command = new MeasureCommand();
    //when
    when(sem6000DeviceMock.isConnected()).thenReturn(false);
    //then
    assertThatCode(() -> sem6000Connection.safeSend(command))
        .isInstanceOf(SendingException.class)
        .hasMessageContaining("not connected");
    assertThat(sem6000Connection.isEstablished()).isFalse();
  }

  @Test
  void reconnects_when_connection_is_lost() throws SendingException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    sem6000Connection.establish();
    Duration reconnectDelay = Duration.ofMillis(20);
    sem6000Connection.setReconnectDelay(reconnectDelay);
    when(sem6000DeviceMock.isConnected()).thenReturn(false);
    MeasureCommand command = new MeasureCommand();
    //when
    reset(sem6000DeviceMock);
    assertThatCode(() -> sem6000Connection.safeSend(command))
        .isInstanceOf(SendingException.class)
        .hasMessageContaining("not connected");
    //then
    await().untilAsserted(() -> verify(sem6000DeviceMock, atLeastOnce()).connect());
  }

  @Test
  @MockitoSettings(strictness = Strictness.LENIENT)
  void keeps_reconnecting_on_runtimeexception_during_reconnect()
      throws Exception {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    Duration reconnectDelay = Duration.ofMillis(20);
    sem6000Connection.setReconnectDelay(reconnectDelay);
    doThrow(RuntimeException.class).when(bluetoothConnectionManagerMock)
        .findDeviceOrFail(anyString(), any(Exception.class));
    //when
    reset(sem6000DeviceMock);
    sem6000Connection.establish();
    //then
    await().untilAsserted(
        () -> verify(bluetoothConnectionManagerMock, atLeast(4)).findDeviceOrFail(anyString(), any(Exception.class)));
  }

  @Test
  void fails_with_sending_exception_when_bluetooth_exception_occurs()
      throws SendingException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    sem6000Connection.establish();
    MeasureCommand command = new MeasureCommand();
    //when
    doThrow(BluezFailedException.class).when(writeService).writeValue(any(), anyMap());
    //then
    assertThatCode(() -> sem6000Connection.safeSend(command))
        .isInstanceOf(SendingException.class)
        .hasMessageContainingAll("Failed", "send", "message");
  }

  @Test
  void fails_with_sending_exception_when_runtime_exception_occurs_during_sending()
      throws SendingException, BluezFailedException, BluezNotAuthorizedException, BluezInvalidValueLengthException, BluezNotSupportedException, BluezInProgressException, BluezNotPermittedException {
    //given
    Sem6000Connection sem6000Connection = new Sem6000Connection(randomSemConfigForPlug("plug1"),
        bluetoothConnectionManagerMock, scheduler);
    bluetoothConnectionManagerMock.setupConnection(sem6000Connection);
    sem6000Connection.establish();
    MeasureCommand command = new MeasureCommand();
    //when
    doThrow(RuntimeException.class).when(writeService).writeValue(any(), anyMap());
    //then
    assertThatCode(() -> sem6000Connection.safeSend(command))
        .isInstanceOf(SendingException.class)
        .hasMessageContainingAll("Failed", "send", "message");
  }
}