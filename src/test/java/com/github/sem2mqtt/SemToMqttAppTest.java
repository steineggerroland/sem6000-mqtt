package com.github.sem2mqtt;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemToMqttAppTest {

  @Test()
  void fails_with_some_kind_of_app_exception_when_config_is_valid() {
    assertThatCode(() -> SemToMqttApp.main(List.of("valid_test.yaml").toArray(new String[1])))
        .isInstanceOf(SemToMqttAppException.class).hasMessageNotContaining("configuration");
  }

  @Test()
  void fails_without_configuration_file() {
    assertThatCode(() -> SemToMqttApp.main(new String[0]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("configuration");
  }
}