package com.github.sem2mqtt.bluetooth;

import static com.github.sem2mqtt.bluetooth.sem6000.Sem6000DbusMessageTestHelper.createMeasurementPropertyChange;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.junit.jupiter.api.Test;

class DevicePropertiesChangedHandlerTest {

  @Test
  void forwards_property_change_on_event_for_dbuspatch() throws DBusException {
    //given
    DevicePropertiesChangedHandler handler = new DevicePropertiesChangedHandler();
    DbusListener listenerMock = mock(DbusListener.class);
    String dbusPath = "/org/bluez/bt1/dev_00_00_00_00_00_01/service000e/char0013";
    PropertiesChanged propertiesChangedEvent = createMeasurementPropertyChange(dbusPath);
    //when
    handler.subscribe(dbusPath, listenerMock);
    handler.handle(propertiesChangedEvent);
    //then
    verify(listenerMock).handle(propertiesChangedEvent);
  }

  @Test
  void does_not_forward_property_change_when_events_dbuspath_does_not_match() throws DBusException {
    //given
    DevicePropertiesChangedHandler handler = new DevicePropertiesChangedHandler();
    DbusListener listenerMock = mock(DbusListener.class);
    String dbusPath = "/org/bluez/bt1/dev_00_00_00_00_00_01/service000e/char0013";
    String dbusPathOfDifferentDevice = "/org/bluez/bt1/dev_12_34_56_78_90_01/service000e/char0013";
    PropertiesChanged propertiesChangedEvent = createMeasurementPropertyChange(dbusPath);
    //when
    handler.subscribe(dbusPathOfDifferentDevice, listenerMock);
    handler.handle(propertiesChangedEvent);
    //then
    verifyNoInteractions(listenerMock);
  }

  @Test
  void ignoring_dbuspath_prevents_forwarding_even_when_matching_message_arrives() throws DBusException {
    //given
    DevicePropertiesChangedHandler handler = new DevicePropertiesChangedHandler();
    DbusListener listenerMock = mock(DbusListener.class);
    String dbusPath = "/org/bluez/bt1/dev_00_00_00_00_00_01/service000e/char0013";
    PropertiesChanged propertiesChangedEvent = createMeasurementPropertyChange(dbusPath);
    //when
    handler.subscribe(dbusPath, listenerMock);
    handler.ignore(dbusPath);
    handler.handle(propertiesChangedEvent);
    //then
    verifyNoInteractions(listenerMock);
  }
}