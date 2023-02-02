package com.github.sem2mqtt.bluetooth.timeflip;

public class GattCharacteristic {

  /**
   * These UUIDs are taken from the time flip documentation. Many UUIDs in the documentation are using upper case
   * letters, whereas bluez uses lower case. We use toLowerCase() to support copying from the documentation without
   * having to bother about the case.
   * <p>
   * Source: time flip documentation,
   * https://github.com/DI-GROUP/TimeFlip.Docs/blob/master/Hardware/TimeFlip%20BLE%20protocol%20ver4_02.06.2020.md
   */
  public static final String SERVICE_GENERAL_ACCESS_UUID = generateGattServiceUuidFor(0x1801).toLowerCase();
  public static final String CHARACTERISTIC_DEVICE_NAME_UUID = generateGattServiceUuidFor(0x2A00).toLowerCase();

  public static final String SERVICE_DEVICE_INFORMATION_UUID = generateGattServiceUuidFor(0x180A).toLowerCase();

  public static final String SERVICE_BATTERY_UUID = generateGattServiceUuidFor(0x180F).toLowerCase();
  public static final String CHARACTERISTIC_BATTERY_LEVEL_UUID = generateGattServiceUuidFor(0x2A19).toLowerCase();

  public static final String SERVICE_TIMEFLIP_UUID = "F1196F50-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_EVENTS_UUID = "F1196F51-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_FACETS_UUID = "F1196F52-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_COMMAND_RESULT_OUTPUT_UUID = "F1196F53-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_COMMAND_UUID = "F1196F54-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_DOUBLE_TAP_UUID = "F1196F55-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_SYSTEM_STATE_UUID = "F1196F56-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_PASSWORD_UUID = "F1196F57-71A4-11E6-BDF4-0800200C9A66".toLowerCase();
  public static final String CHARACTERISTIC_HISTORY_UUID = "F1196F58-71A4-11E6-BDF4-0800200C9A66".toLowerCase();

  private GattCharacteristic() {
    //hide constructor of helper
  }

  private static String generateGattServiceUuidFor(int serviceId) {
    return String.format("%08x-0000-1000-8000-00805f9b34fb", serviceId);
  }
}
