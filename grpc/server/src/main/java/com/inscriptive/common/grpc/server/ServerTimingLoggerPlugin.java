package com.inscriptive.common.grpc.server;

import com.google.common.base.Stopwatch;
import com.inscriptive.common.Logger;
import com.inscriptive.common.grpc.shared.ContextKeys;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Logs the elapsed time of {@link ContextKeys#SERVER_STOPWATCH}
 */
public class ServerTimingLoggerPlugin extends Logger.ThreadContextPlugin {
  public static final ServerTimingLoggerPlugin INSTANCE = new ServerTimingLoggerPlugin();

  protected ServerTimingLoggerPlugin() {
    super("serverElapsedMillis");
  }

  @Override
  protected String contextValue() {
    Stopwatch stopwatch = ContextKeys.SERVER_STOPWATCH.get();
    return stopwatch == null
        ? null
        : Long.toString(stopwatch.elapsed(MILLISECONDS));
  }
}
