package com.github.sem2mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.Invocation;
import org.mockito.verification.VerificationMode;
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

  public static VerificationMode atLeastOneMatches() {
    return data -> {
      List<String> failedAssertions = new CopyOnWriteArrayList<>();
      data.getAllInvocations().stream().filter(inv -> {
            try {
              data.getTarget().matches(inv);
              return true;
            } catch (AssertionError e) {
              failedAssertions.add(e.getMessage());
              return false;
            }
          }).findFirst()
          .ifPresentOrElse((c) -> {
            data.getAllInvocations().forEach(Invocation::markVerified);
          }, () -> {
            throw new AssertionError("None of the log entries matches: \n" + String.join("\n", failedAssertions));
          });
    };
  }
}
