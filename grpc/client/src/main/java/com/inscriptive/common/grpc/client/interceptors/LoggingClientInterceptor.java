package com.inscriptive.common.grpc.client.interceptors;

import com.inscriptive.common.Logger;
import io.grpc.*;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.grpc.Status.Code.OK;

public class LoggingClientInterceptor implements ClientInterceptor {
  private static final Logger logger = Logger.getLogger(LoggingClientInterceptor.class);
  public static final LoggingClientInterceptor INSTANCE = new LoggingClientInterceptor();

  private LoggingClientInterceptor() {}

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        channel.newCall(methodDescriptor, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        logger.debug("outbound gRPC method: [%s]", methodDescriptor.getFullMethodName());
        super.start(new LoggingClientCallListener<>(responseListener), headers);
      }

      @Override
      public void cancel(String message, Throwable t) {
        logger.debug("call cancelled by client with message: %s", message);
        logger.debug("call cancellation throwable from client: %s", getStackTraceAsString(t));
        super.cancel(message, t);
      }

      @Override
      public void halfClose() {
        logger.debug("client finished sending messages");
        super.halfClose();
      }
    };
  }

  private static class LoggingClientCallListener<RespT> extends
      ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
    private LoggingClientCallListener(ClientCall.Listener<RespT> delegate) {
      super(delegate);
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
      if (status.getCode() == OK) {
        logger.debug("call was completed with status OK");
      } else {
        logger.warn("call was completed with status %s", status);
      }
      super.onClose(status, trailers);
    }
  }
}
