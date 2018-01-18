package com.inscriptive.common.grpc.server.interceptors;

import com.inscriptive.common.grpc.shared.ContextKeys;
import com.inscriptive.common.grpc.shared.MetadataKeys;
import io.grpc.*;

/**
 * Attaches the trace id passed through the current request to the current {@link Context}. If
 * there is no trace id on the request, attaches a sentinel value.
 */
public class TracingServerInterceptor implements ServerInterceptor {
  public static final TracingServerInterceptor INSTANCE = new TracingServerInterceptor();

  private TracingServerInterceptor() {}

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    // this may be null if the request originated from a client that did not have the proper
    // interceptor installed, in that case we use a sentinel value
    String traceId = metadata.get(MetadataKeys.TRACE_ID);
    return Contexts.interceptCall(Context.current().withValue(ContextKeys.TRACE_ID,
        traceId == null ? "no_trace_id" : traceId),
        serverCall, metadata, serverCallHandler);
  }
}
