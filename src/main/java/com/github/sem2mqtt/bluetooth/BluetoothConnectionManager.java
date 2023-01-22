package com.github.sem2mqtt.bluetooth;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.sem2mqtt.SemToMqttAppException;
import com.github.sem2mqtt.bluetooth.DevicePropertiesChangedHandler.DbusListener;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothConnectionManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothConnectionManager.class);
  private final DeviceManager deviceManager;
  private final DevicePropertiesChangedHandler dbusPathHandler;

  public BluetoothConnectionManager(DeviceManager deviceManager) {
    this.deviceManager = deviceManager;
    this.dbusPathHandler = new DevicePropertiesChangedHandler();
  }

  public void init() {

    try {
      deviceManager.registerPropertyHandler(dbusPathHandler);
    } catch (DBusException e) {
      throw new SemToMqttAppException("Failed to initialize bluetooth device manager", e);
    }
    deviceManager.scanForBluetoothDevices(10 * 1000);
  }

  public <T extends BluetoothConnection> T setupConnection(T bluetoothConnection) {
    LOGGER.debug("Registered bluetooth connection for mac address '{}'", bluetoothConnection.getMacAddress());
    return bluetoothConnection;
  }

  public <T extends Exception> BluetoothDevice findDeviceOrFail(String macAddress, T e) throws T {
    return deviceManager.getDevices().stream()
        .filter(bluetoothDevice -> bluetoothDevice.getAddress().equals(macAddress)).findFirst().orElseThrow(() -> e);
  }

  public void subscribeToDbusPath(String dbusPath, DbusListener listener) {
    dbusPathHandler.subscribe(dbusPath, listener);
  }

  public void ignoreDbusPath(String dbusPath) {
    dbusPathHandler.ignore(dbusPath);
  }
}
