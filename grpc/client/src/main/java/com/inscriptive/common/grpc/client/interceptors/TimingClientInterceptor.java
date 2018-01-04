package com.inscriptive.common.grpc.client.interceptors;

import com.inscriptive.common.grpc.shared.ContextKeys;
import io.grpc.*;

public class TimingClientInterceptor implements ClientInterceptor {
  public static final TimingClientInterceptor INSTANCE = new TimingClientInterceptor();

  private TimingClientInterceptor() {}

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                             CallOptions callOptions, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        TimingClientCallListener<RespT> decoratedListener = new TimingClientCallListener<>(
            responseListener);
        ContextKeys.CLIENT_STOPWATCH.get().start();
        super.start(decoratedListener, headers);
      }
    };
  }

  private static class TimingClientCallListener<RespT>
      extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {

    private TimingClientCallListener(ClientCall.Listener<RespT> delegate) {
      super(delegate);
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
      ContextKeys.CLIENT_STOPWATCH.get().stop();
      super.onClose(status, trailers);
    }
  }
}
