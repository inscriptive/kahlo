package com.inscriptive.common.grpc.server.interceptors;

import com.google.common.base.Stopwatch;
import com.inscriptive.common.grpc.shared.ContextKeys;
import io.grpc.*;

/**
 * Server interceptor that adds a started stopwatch to the call context
 */
public class TimingServerInterceptor implements ServerInterceptor {
  public static final TimingServerInterceptor INSTANCE = new TimingServerInterceptor();

  private TimingServerInterceptor() {}
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    return Contexts.interceptCall(
        Context.current().withValue(ContextKeys.SERVER_STOPWATCH, stopwatch),
        serverCall, metadata, serverCallHandler);
  }
}
