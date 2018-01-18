package com.inscriptive.common.grpc.server.interceptors;

import com.inscriptive.common.Logger;
import io.grpc.*;

/**
 * Interceptor that logs when a call starts getting handled, and when it is either cancelled or
 * completed.
 */
public class LoggingServerInterceptor implements ServerInterceptor {
  public static final LoggingServerInterceptor INSTANCE = new LoggingServerInterceptor();
  private static final Logger logger = Logger.getLogger(LoggingServerInterceptor.class);

  private LoggingServerInterceptor() {}

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    logger.debug("inbound gRPC method: [%s]",
        serverCall.getMethodDescriptor().getFullMethodName());
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
        next.startCall(new LoggingServerCall<>(serverCall), metadata)) {

      @Override
      public void onHalfClose() {
        logger.debug("client finished sending messages");
        super.onHalfClose();
      }

      @Override
      public void onCancel() {
        logger.warn("call was cancelled");
        super.onCancel();
      }

      @Override
      public void onComplete() {
        logger.debug("call was completed");
        super.onComplete();
      }
    };
  }

  private static class LoggingServerCall<ReqT, RespT> extends
      ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
    protected LoggingServerCall(ServerCall<ReqT, RespT> delegate) {
      super(delegate);
    }

    @Override
    public void close(Status status, Metadata trailers) {
      if (status.isOk()) {
        logger.debug("call was closed with status OK");
      } else {
        // Log server rpc errors as exceptions so we can get stack traces.
        // Clients are free to swallow the errors as they see fit (e.g. silently retry).
        // Handle duplicates (client / server both report the error) in the alerting tool.
        // TODO(shawn) filter out client error statuses and only report server statuses.
        logger.error("call was closed with status %s", status.asException());
      }
      super.close(status, trailers);
    }
  }
}
