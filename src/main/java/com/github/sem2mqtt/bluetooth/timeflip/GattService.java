package com.github.sem2mqtt.bluetooth.timeflip;

import static java.util.Collections.emptyList;

import java.util.List;

/**
 * The services of time flip according to its documentation with a list of its characteristics. Many UUIDs in the
 * documentation are using upper case letters, whereas bluez uses lower case. We use toLowerCase() to support copying
 * from the documentation without having to bother about the case.
 * <p>
 * Source: <a
 * href="https://github.com/DI-GROUP/TimeFlip.Docs/blob/master/Hardware/TimeFlip%20BLE%20protocol%20ver4_02.06.2020.md">
 * time flip documentation
 * </a>
 */
public enum GattService {
  GENERAL_ACCESS_SERVICE(generateGattServiceUuidFor(0x1801).toLowerCase(), List.of(GattCharacteristic.DEVICE_NAME)),
  DEVICE_INFORMATION_SERVICE(generateGattServiceUuidFor(0x180A).toLowerCase(), emptyList()),
  SERVICE_BATTERY_UUID(generateGattServiceUuidFor(0x180F).toLowerCase(), List.of(GattCharacteristic.BATTERY_LEVEL)),
  TIMEFLIP("F1196F50-71A4-11E6-BDF4-0800200C9A66".toLowerCase(),
      List.of(GattCharacteristic.EVENTS_UUID, GattCharacteristic.FACETS_UUID,
          GattCharacteristic.COMMAND_RESULT_OUTPUT_UUID, GattCharacteristic.COMMAND_UUID,
          GattCharacteristic.DOUBLE_TAP_UUID, GattCharacteristic.SYSTEM_STATE_UUID, GattCharacteristic.PASSWORD_UUID,
          GattCharacteristic.HISTORY_UUID));

  public final String uuid;
  public final List<GattCharacteristic> characteristics;

  GattService(String uuid, List<GattCharacteristic> characteristics) {
    this.uuid = uuid;
    this.characteristics = characteristics;
  }


  private static String generateGattServiceUuidFor(int serviceId) {
    return String.format("%08x-0000-1000-8000-00805f9b34fb", serviceId);
  }
}
