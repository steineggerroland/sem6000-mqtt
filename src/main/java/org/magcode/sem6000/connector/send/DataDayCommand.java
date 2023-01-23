package org.magcode.sem6000.connector.send;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;

public class DataDayCommand extends Command {

  public DataDayCommand() throws SendingException {
    byte[] message = getMessage("0a", "000000");
    setMessage(message);
  }
}