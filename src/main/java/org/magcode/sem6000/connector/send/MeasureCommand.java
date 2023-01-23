package org.magcode.sem6000.connector.send;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;

public class MeasureCommand extends Command {

  public MeasureCommand() throws SendingException {
    byte[] message = getMessage("04", "000000");
    setMessage(message);
  }
}