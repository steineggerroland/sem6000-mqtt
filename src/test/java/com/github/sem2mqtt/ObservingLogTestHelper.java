package com.github.sem2mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;

public class ObservingLogTestHelper {

  private ObservingLogTestHelper() {
    //avoid instantiation of helper
  }

  public static Appender<ILoggingEvent> observeLogsOf(Class<?> classToObserveLogsOf) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = loggerContext.getLogger(classToObserveLogsOf);
    Appender<ILoggingEvent> logAppenderMock = mock(Appender.class);
    logger.addAppender(logAppenderMock);
    return logAppenderMock;
  }

  public static ArgumentMatcher<ILoggingEvent> logs(String... expectedStringsInLog) {
    return logArgument -> {
      AbstractStringAssert<?> abstractStringAssert = assertThat(logArgument)
          .asInstanceOf(InstanceOfAssertFactories.type(ILoggingEvent.class))
          .extracting(ILoggingEvent::getFormattedMessage).asString();
      for (String expectedMessage : expectedStringsInLog) {
        abstractStringAssert.containsIgnoringCase(expectedMessage);
      }
      return true;
    };
  }
}
