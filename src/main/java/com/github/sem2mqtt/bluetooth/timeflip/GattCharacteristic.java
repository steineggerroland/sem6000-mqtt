package com.github.sem2mqtt.bluetooth.timeflip;

/**
 * The  characteristics of time flip according to its documentation. Many UUIDs in the documentation are using upper
 * case letters, whereas bluez uses lower case. We use toLowerCase() to support copying from the documentation without
 * having to bother about the case.
 * <p>
 * Source: <a
 * href="https://github.com/DI-GROUP/TimeFlip.Docs/blob/master/Hardware/TimeFlip%20BLE%20protocol%20ver4_02.06.2020.md">
 * time flip documentation
 * </a>
 */

public enum GattCharacteristic {
  // Characteristics of general access service
  DEVICE_NAME(generateGattServiceUuidFor(0x2A00).toLowerCase()),
  // Characteristics of battery service

  BATTERY_LEVEL(generateGattServiceUuidFor(0x2A19).toLowerCase()),
  // Characteristics of timeflip service
  EVENTS_UUID("F1196F51-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  FACETS_UUID("F1196F52-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  COMMAND_RESULT_OUTPUT_UUID("F1196F53-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  COMMAND_UUID("F1196F54-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  DOUBLE_TAP_UUID("F1196F55-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  SYSTEM_STATE_UUID("F1196F56-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  PASSWORD_UUID("F1196F57-71A4-11E6-BDF4-0800200C9A66".toLowerCase()),
  HISTORY_UUID("F1196F58-71A4-11E6-BDF4-0800200C9A66".toLowerCase());

  public final String uuid;

  GattCharacteristic(String uuid) {
    this.uuid = uuid;
  }

  private static String generateGattServiceUuidFor(int serviceId) {
    return String.format("%08x-0000-1000-8000-00805f9b34fb", serviceId);
  }
}