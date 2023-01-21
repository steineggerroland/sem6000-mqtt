package com.github.sem2mqtt;


public class SemToMqttAppException extends RuntimeException {

  public SemToMqttAppException(String message, Exception cause) {
    super(String.format("Critical SemToMqttApp exception: %s", message), cause);
  }
}
