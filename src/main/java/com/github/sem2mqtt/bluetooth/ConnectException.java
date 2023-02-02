package com.github.sem2mqtt.bluetooth;

public class ConnectException extends Exception {

  public ConnectException(String message) {
    super(message);
  }

  public ConnectException(Exception e) {
    super(e);
  }
}
