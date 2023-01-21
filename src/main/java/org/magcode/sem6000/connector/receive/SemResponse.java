package org.magcode.sem6000.connector.receive;

public abstract class SemResponse {

  private final ResponseType responseType;

  protected SemResponse(ResponseType responseType) {
    this.responseType = responseType;
  }

  public ResponseType getType() {
    return responseType;
  }
}
