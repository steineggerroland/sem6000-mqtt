package com.github.sem2mqtt.bluetooth;

import static com.github.sem2mqtt.ObservingLogTestHelper.logs;
import static com.github.sem2mqtt.ObservingLogTestHelper.observeLogsOf;
import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.DBUS_PATH_01;
import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.createMeasurementPropertyChange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.sem2mqtt.SemToMqttAppException;
import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import java.util.List;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BluetoothConnectionManagerTest {

  @Captor
  ArgumentCaptor<DevicePropertiesChangedHandler> deviceChangeHandlerCaptor;

  @Test
  void scans_for_devices_when_initializing() {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    //when
    bluetoothConnectionManager.init();
    //then
    verify(deviceManagerMock).scanForBluetoothDevices(anyInt());
  }

  @Test
  void subscribes_handler_when_initializing() throws DBusException {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    //when
    bluetoothConnectionManager.init();
    //then
    verify(deviceManagerMock).registerPropertyHandler(any(DevicePropertiesChangedHandler.class));
  }

  @Test
  void crashes_app_when_bluetooth_lib_fails() throws DBusException {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    //when
    doThrow(DBusException.class).when(deviceManagerMock).registerPropertyHandler(any());
    //then
    assertThatCode(bluetoothConnectionManager::init).isInstanceOf(SemToMqttAppException.class)
        .hasMessageContaining("Failed")
        .hasMessageContaining("bluetooth");
  }

  @Test
  void logs_macaddress_when_setting_up_connection() {
    //given
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(mock(DeviceManager.class));
    BluetoothConnection bluetoothConnectionMock = mock(BluetoothConnection.class);
    String macAddress = "12:34:12:34:56:78:00";
    when(bluetoothConnectionMock.getMacAddress()).thenReturn(macAddress);
    Appender<ILoggingEvent> logObserver = observeLogsOf(BluetoothConnectionManager.class);
    //when
    bluetoothConnectionManager.setupConnection(bluetoothConnectionMock);
    //then
    verify(logObserver).doAppend(argThat(logs(macAddress)));
  }

  @Test
  void returns_matching_device_when_device_manager_has_it() throws Exception {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    String macAddress = "00:11:22:33:44:55:66";
    //when
    BluetoothDevice bluetoothDeviceMock = mock(BluetoothDevice.class);
    when(deviceManagerMock.getDevices()).thenReturn(List.of(bluetoothDeviceMock));
    when(bluetoothDeviceMock.getAddress()).thenReturn(macAddress);
    //then
    assertThat(bluetoothConnectionManager.findDeviceOrFail(macAddress, mock())).isEqualTo(bluetoothDeviceMock);
  }

  @Test
  void fails_with_exception_when_device_manager_has_no_matching_device() throws Exception {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    String unknownMacAddress = "00:11:22:33:44:55:66";
    //when
    when(deviceManagerMock.getDevices()).thenReturn(List.of(mock(BluetoothDevice.class, RETURNS_MOCKS)));
    //then
    RuntimeException wantedException = mock();
    assertThatCode(() -> bluetoothConnectionManager.findDeviceOrFail(unknownMacAddress, wantedException))
        .isEqualTo(wantedException);
  }

  @Test
  void forwards_property_change_when_subscribing_for_dbuspath() throws DBusException {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    bluetoothConnectionManager.init();
    verify(deviceManagerMock).registerPropertyHandler(deviceChangeHandlerCaptor.capture());
    PropertiesChanged measurementPropertyChange = createMeasurementPropertyChange(DBUS_PATH_01);
    DbusListener listenerMock = mock(DbusListener.class);
    //when
    bluetoothConnectionManager.subscribeToDbusPath(DBUS_PATH_01, listenerMock);
    deviceChangeHandlerCaptor.getValue().handle(measurementPropertyChange);
    //then
    verify(listenerMock).handle(measurementPropertyChange);
  }

  @Test
  void does_not_forward_property_change_when_ignoring_dbuspath() throws DBusException {
    //given
    DeviceManager deviceManagerMock = mock(DeviceManager.class);
    BluetoothConnectionManager bluetoothConnectionManager = new BluetoothConnectionManager(deviceManagerMock);
    bluetoothConnectionManager.init();
    verify(deviceManagerMock).registerPropertyHandler(deviceChangeHandlerCaptor.capture());
    PropertiesChanged measurementPropertyChange = createMeasurementPropertyChange(DBUS_PATH_01);
    DbusListener listenerMock = mock(DbusListener.class);
    //when
    bluetoothConnectionManager.subscribeToDbusPath(DBUS_PATH_01, listenerMock);
    bluetoothConnectionManager.ignoreDbusPath(DBUS_PATH_01);
    deviceChangeHandlerCaptor.getValue().handle(measurementPropertyChange);
    //then
    verifyNoInteractions(listenerMock);
  }
}
