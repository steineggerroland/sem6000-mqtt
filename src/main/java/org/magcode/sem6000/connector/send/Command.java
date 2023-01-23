package org.magcode.sem6000.connector.send;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.magcode.sem6000.connector.ByteUtils;

public abstract class Command {

  private byte[] message;

  protected Command() {

  }

  protected Command(byte[] message) {
    setMessage(message);
  }

  public byte[] getMessage() {
    return message;
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }


  public static byte[] buildMessage(String command, byte[] payload) throws SendingException {
    byte[] bcom = hexStringToByteArray(command);
    byte[] bstart = hexStringToByteArray("0f");
    byte[] bend = hexStringToByteArray("ffff");
    int len = 1 + payload.length + bcom.length;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      outputStream.write(bstart);
      outputStream.write((byte) len);
      outputStream.write(bcom);
      outputStream.write(payload);
      outputStream.write((byte) 0x00);
      outputStream.write(bend);
    } catch (IOException e) {
      throw new SendingException("Failed to build message, ", e);
    }

    byte[] checksum = outputStream.toByteArray();

    int currentValue = 1;

    for (int i = 2; i < len + 1; i++) {
      currentValue = currentValue + checksum[i];
    }
    checksum[checksum.length - 3] = (byte) currentValue;
    return checksum;
  }

  public static byte[] getMessage(String command, String payload) throws SendingException {
    byte[] bpay = hexStringToByteArray(payload);
    return buildMessage(command, bpay);

  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  public String getReadableMessage() {
    return ByteUtils.byteArrayToHex(this.message);
  }
}
