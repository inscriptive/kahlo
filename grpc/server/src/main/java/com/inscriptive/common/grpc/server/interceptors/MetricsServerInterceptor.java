package com.inscriptive.common.grpc.server.interceptors;

import com.codahale.metrics.MetricRegistry;
import com.inscriptive.common.grpc.shared.ContextKeys;
import io.grpc.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class MetricsServerInterceptor implements ServerInterceptor {
  private final MetricRegistry metricRegistry;

  @Inject
  public MetricsServerInterceptor(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  // TODO(jack) add other metrics such as byte counts, etc.
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    String calls = MetricRegistry.name(
        serverCall.getMethodDescriptor().getFullMethodName(),
        "calls");
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
        next.startCall(new MetricsServerCall<>(serverCall), metadata)) {

      @Override
      public void onComplete() {
        super.onComplete();
        long elapsedMillis = ContextKeys.SERVER_STOPWATCH.get().elapsed(MILLISECONDS);
        metricRegistry.timer(calls).update(elapsedMillis, MILLISECONDS);
      }
    };
  }

  private static class MetricsServerCall<ReqT, RespT> extends
      ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
    protected MetricsServerCall(ServerCall<ReqT, RespT> delegate) {
      super(delegate);
    }

    @Override
    public void close(Status status, Metadata trailers) {
      super.close(status, trailers);
    }
  }
}
