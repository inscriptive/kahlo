package com.inscriptive.common.grpc.client.interceptors;

import com.inscriptive.common.grpc.shared.ContextKeys;
import com.inscriptive.common.grpc.shared.MetadataKeys;
import io.grpc.*;

/**
 * Populates headers with the current trace id.
 */
public class TracingClientInterceptor implements ClientInterceptor {
  public static final TracingClientInterceptor INSTANCE = new TracingClientInterceptor();

  private TracingClientInterceptor() {}

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                             CallOptions callOptions, Channel next) {
    return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      @Override
      protected void checkedStart(Listener<RespT> responseListener, Metadata headers)
          throws Exception {
        headers.put(MetadataKeys.TRACE_ID, ContextKeys.TRACE_ID.get());
        delegate().start(responseListener, headers);
      }
    };
  }
}
