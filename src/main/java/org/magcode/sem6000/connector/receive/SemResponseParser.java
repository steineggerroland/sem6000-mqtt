package org.magcode.sem6000.connector.receive;

import org.magcode.sem6000.connector.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemResponseParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemResponseParser.class);

  public static SemResponse parseMessage(byte[] message) {
    LOGGER.atDebug().log(() -> String.format("Parsing sem6000 message '%s'", ByteUtils.byteArrayToHex(message)));
    if (message[3] != (byte) 0x00) {
      LOGGER.debug("Message is unknown.");
      return new UnknownResponse();
    }

    if (message[0] != (byte) 0x0f) {
      return new IncompleteResponse(message);
    }

    int expectedLen = message[1] & 0xFF;
    int actualLen = message.length;
    if (!(actualLen - expectedLen - 4 == 0 || actualLen - expectedLen - 2 == 0)) {
      LOGGER.debug("Message is not complete. Expected length: {}, actual length: {}.", expectedLen, actualLen);
      return new IncompleteResponse(message);
    }

    switch (message[2]) {
      case (byte) 0x17:
        return new LoginResponse(message[4]);
      case (byte) 0x04:
        return handleMeasurementResponse(message);
      case (byte) 0x0a:
        return handleDataDayResponse(message, actualLen);
      case (byte) 0x01:
        return new SyncTimeResponse(message[4]);
      case (byte) 0x03:
        return new SwitchResponse(message[4]);
      case (byte) 0x0f:
        return new LedResponse();
      default:
        return new UnknownResponse();
    }
  }

  private static MeasurementResponse handleMeasurementResponse(byte[] message) {
    byte[] measurement = new byte[48];
    System.arraycopy(message, 4, measurement, 0, 14);
    return new MeasurementResponse(measurement);
  }

  private static SemResponse handleDataDayResponse(byte[] message, int actualLen) {
    if (actualLen == 55 && message[actualLen - 1] == (byte) 0xff) {
      byte[] dataday = new byte[48];
      System.arraycopy(message, 4, dataday, 0, 48);
      return new DataDayResponse(dataday);
    } else {
      return new UnknownResponse();
    }
  }
}
