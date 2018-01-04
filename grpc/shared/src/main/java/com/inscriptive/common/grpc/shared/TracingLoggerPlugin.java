package com.inscriptive.common.grpc.shared;

import com.inscriptive.common.Logger;

public class TracingLoggerPlugin extends Logger.ThreadContextPlugin {
  public static final TracingLoggerPlugin INSTANCE = new TracingLoggerPlugin();

  private TracingLoggerPlugin() {
    super("traceId");
  }

  @Override
  protected String contextValue() {
    return ContextKeys.TRACE_ID.get();
  }
}
