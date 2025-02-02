package org.magcode.sem6000.connector.send;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;

public class SwitchCommand extends Command {

  private final boolean state;

  public SwitchCommand(boolean state) throws SendingException {
    this.state = state;
    byte[] payload = new byte[3];
    if (state) {
      payload[0] = (byte) 0x01;
    }

    byte[] message = buildMessage("0300", payload);
    setMessage(message);
  }

  @Override
  public String toString() {
    return String.format("led %s", state ? "on" : "off");
  }
}
