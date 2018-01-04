package com.inscriptive.common.grpc.client;

import com.google.common.base.Stopwatch;
import com.inscriptive.common.Logger;
import com.inscriptive.common.grpc.shared.ContextKeys;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Logs the elapsed time of {@link ContextKeys#CLIENT_STOPWATCH}
 */
public class ClientTimingLoggerPlugin extends Logger.ThreadContextPlugin {
  public static final ClientTimingLoggerPlugin INSTANCE = new ClientTimingLoggerPlugin();

  protected ClientTimingLoggerPlugin() {
    super("clientElapsedMillis");
  }

  @Override
  protected String contextValue() {
    Stopwatch stopwatch = ContextKeys.CLIENT_STOPWATCH.get();
    return stopwatch == null
        ? null
        : Long.toString(stopwatch.elapsed(MILLISECONDS));
  }
}
