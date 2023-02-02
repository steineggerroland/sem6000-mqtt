package com.github.sem2mqtt.bluetooth.timeflip;

import static com.github.sem2mqtt.bluetooth.timeflip.GattCharacteristic.SERVICE_TIMEFLIP_UUID;

import com.coreoz.wisp.Scheduler;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.github.sem2mqtt.bluetooth.BluetoothConnection;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeflipConnection extends BluetoothConnection {

  public static final Logger LOGGER = LoggerFactory.getLogger(TimeflipConnection.class);
  private final TimeFlipConfiguration timeFlipConfiguration;

  private BluetoothDevice device;

  public TimeflipConnection(TimeFlipConfiguration timeFlipConfiguration, BluetoothConnectionManager bluetoothConnection,
      Scheduler scheduler) {
    super(bluetoothConnection, scheduler);
    this.timeFlipConfiguration = timeFlipConfiguration;
  }

  public void establish() {
    LOGGER.info("Establishing connection to {}", timeFlipConfiguration.getMac());
    try {
      connectToDevice();
    } catch (ConnectException e) {
      LOGGER.warn("Device {} is not connected properly. Scheduling reconnect. ", timeFlipConfiguration.getMac());
      LOGGER.debug("Reason: ", e);
      scheduleReconnect();
    } catch (RuntimeException e) {
      LOGGER.warn("General exception, device {} is not connected properly. Scheduling reconnect. ",
          timeFlipConfiguration.getMac());
      scheduleReconnect();
    }
  }

  private void connectToDevice() throws ConnectException {
    device = connectionManager.findDeviceOrFail(timeFlipConfiguration.getMac(),
        new ConnectException("Could not find device."));
    if (!device.connect()) {
      throw new ConnectException("Could not connect to device.");
    }
    try {
      connectionManager.subscribeToDbusPath(device.getDbusPath(), changedProperties -> {
        LOGGER.debug(changedProperties.toString());
      });

      device.getGattServices()
          .forEach(bluetoothGattService -> LOGGER.debug("{} {}", bluetoothGattService.getUuid(), bluetoothGattService));

      BluetoothGattService timeFlipService = device.getGattServiceByUuid(SERVICE_TIMEFLIP_UUID.toLowerCase());
      timeFlipService.getGattCharacteristics()
          .forEach(bluetoothGattChar -> LOGGER.debug("{} {}", bluetoothGattChar.getUuid(), bluetoothGattChar));
    } catch (RuntimeException e) {
      throw new ConnectException(e);
    }
  }

  private void scheduleReconnect() {

  }

  @Override
  public String getMacAddress() {
    return timeFlipConfiguration.getMac();
  }
}
