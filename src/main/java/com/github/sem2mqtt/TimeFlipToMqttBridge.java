package com.github.sem2mqtt;

import com.coreoz.wisp.Scheduler;
import com.github.sem2mqtt.bluetooth.BluetoothConnectionManager;
import com.github.sem2mqtt.bluetooth.timeflip.TimeFlipConfiguration;
import com.github.sem2mqtt.bluetooth.timeflip.TimeflipConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeFlipToMqttBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeFlipToMqttBridge.class);
  private final TimeflipConnection timeflipConnection;

  public TimeFlipToMqttBridge(TimeFlipConfiguration configuration, BluetoothConnectionManager connectionManager,
      Scheduler scheduler) {
    this.timeflipConnection = new TimeflipConnection(configuration, connectionManager, scheduler);
  }

  public void run() {
    LOGGER.info("Starting time flip ({}) to mqtt bridge", timeflipConnection.getMacAddress());
    timeflipConnection.establish();
  }
}
