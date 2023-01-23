package org.magcode.sem6000.connector.receive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createIncompleteResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createMeasureResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemDayDataResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemResponseFor;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSyncTimeResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createUnknownSemResponse;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.magcode.sem6000.connector.send.Command;

class SemResponseParserTest {

  @Test
  void testSynTime() {
    SemResponse semResponse = createSyncTimeResponse();
    assertEquals(ResponseType.SYNCTIME, semResponse.getType());
  }

  @Test
  void type_login_when_login_response() {
    SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray("0f01170001"));
    assertThat(semResponse)
        .usingRecursiveComparison()
        .isEqualTo(new LoginResponse(Byte.parseByte("01")));
    assertThat(semResponse.getType()).isEqualTo(ResponseType.LOGIN);
  }

  @Test
  void type_led_when_led_response() {
    SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray("0f010f0000"));
    assertThat(semResponse)
        .usingRecursiveComparison()
        .isEqualTo(new LedResponse());
    assertThat(semResponse.getType()).isEqualTo(ResponseType.LED);
  }

  @Test
  void type_switch_when_switch_response() {
    SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray("0f01030001"));
    assertThat(semResponse)
        .usingRecursiveComparison()
        .isEqualTo(new SwitchResponse(Byte.parseByte("01")));
    assertThat(semResponse.getType()).isEqualTo(ResponseType.SWITCHRELAY);
  }

  @Test
  void wrongOrderDay() {
    SemResponse semResponse = createUnknownSemResponse();
    assertEquals(ResponseType.UNKNOWN, semResponse.getType());
  }

  @Test
  void dataDayTest() {
    SemResponse semResponse = createSemDayDataResponse();
    assertEquals(ResponseType.DATADAY, semResponse.getType());
    DataDayResponse dataResp = (DataDayResponse) semResponse;
    assertEquals(1638, dataResp.getLast24h());
    assertEquals(1638, dataResp.getToday());
  }

  @Test
  void testMeasure() {
    SemResponse semResponse = createMeasureResponse();
    assertEquals(ResponseType.MEASURE, semResponse.getType());
    MeasurementResponse mRes = (MeasurementResponse) semResponse;
    assertEquals(234, mRes.getVoltage());
    assertEquals(11.464, mRes.getPower(), 0.001f);
  }

  @Test
  void testIncompleteDay1() {
    SemResponse semResponse = createSemResponseFor("0f330a0000000000000000000000000000000000");
    assertEquals(ResponseType.INCOMPLETE, semResponse.getType());
  }

  @Test
  void testIncompleteDay2() {
    SemResponse semResponse = createIncompleteResponse();
    assertEquals(ResponseType.INCOMPLETE, semResponse.getType());
  }

  @Test
  void testIncompleteDay3() {
    SemResponse semResponse = createSemResponseFor(
        "0f330a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000bffff");
    assertEquals(ResponseType.DATADAY, semResponse.getType());
  }

  @Test
  void response_is_unknown_when_message_is_arbitrary() {
    assertThat(SemResponseParser.parseMessage("apfel".getBytes(StandardCharsets.UTF_8)))
        .usingRecursiveComparison()
        .isEqualTo(new UnknownResponse());
  }
}
