package com.github.sem2mqtt.bluetooth.sem6000;

public enum Sem6000GattCharacteristic {
  SERVICE("0000fff0-0000-1000-8000-00805f9b34fb"),
  WRITE("0000fff3-0000-1000-8000-00805f9b34fb"),
  NOTIFY("0000fff4-0000-1000-8000-00805f9b34fb");


  public final String uuid;

  Sem6000GattCharacteristic(String uuid) {
    this.uuid = uuid;
  }

}
