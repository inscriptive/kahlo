package com.inscriptive.common.grpc.client.interceptors;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.inscriptive.common.grpc.shared.ContextKeys;
import com.inscriptive.common.util.UUID;
import io.grpc.*;

import java.util.List;
import java.util.Optional;

/**
 * This class deserves a lot of explanation. Our client interceptors share some state. For example,
 * the timing interceptor needs to work with the same stopwatch that the logging interceptor is
 * recording timings from. Therefore, we need a parent interceptor to coordinate that state, since
 * the interceptors can't talk to each other.
 *
 * Where it gets difficult is in the logging. Our logger uses the ThreadContext to log things like
 * the trace id and the elapsed call time. This uses thread local state. However, we don't know
 * what thread is going to be calling our interceptor methods. The best alternative is to use a
 * gRPC Context, and wrap the entire interceptor chain in the context.
 *
 * The final piece is that the methods of {@link ClientCall} are intercepted in reverse order from
 * the methods of {@link ClientCall.Listener}. That means that we need an Outbound interceptor to
 * contextualize the outbound calls, and an inbound interceptor for the inbound (listener) calls.
 *
 * Yes, this all actually works.
 */
public class ParentClientInterceptor implements ClientInterceptor {
  private final List<ClientInterceptor> interceptors;

  public ParentClientInterceptor(List<ClientInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                             CallOptions callOptions, Channel next) {
    Context interceptionContext = Context.current();

    // timer
    Stopwatch stopwatch = Stopwatch.createUnstarted();
    interceptionContext = interceptionContext.withValue(ContextKeys.CLIENT_STOPWATCH, stopwatch);

    // trace id
    Optional<String> currentTraceId = Optional.ofNullable(ContextKeys.TRACE_ID.get());
    if (!currentTraceId.isPresent()) {
      // If there's no trace id in the current context then we must be a leaf node, and we
      // generate our own!
      String traceId = UUID.get().toString().substring(0, 8);
      interceptionContext = interceptionContext.withValue(ContextKeys.TRACE_ID, traceId);
    }

    // interceptors are applied from the bottom up
    return ClientInterceptors.intercept(next,
        ImmutableList.<ClientInterceptor>builder()
            .add(new InboundContextualizedClientInterceptor(interceptionContext))
            .addAll(interceptors)
            .add(IdentityPropagatingClientInterceptor.INSTANCE)
            .add(LoggingClientInterceptor.INSTANCE)
            .add(TimingClientInterceptor.INSTANCE)
            .add(TracingClientInterceptor.INSTANCE)
            .add(new OutboundContextualizedClientInterceptor(interceptionContext))
            .build()
    ).newCall(method, callOptions);
  }

  private static class OutboundContextualizedClientInterceptor implements ClientInterceptor {
    private final Context context;

    private OutboundContextualizedClientInterceptor(Context context) {
      this.context = context;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          context.wrap(() -> super.start(responseListener, headers)).run();
        }

        @Override
        public void request(int numMessages) {
          context.wrap(() -> super.request(numMessages)).run();
        }

        @Override
        public void cancel(String message, Throwable throwable) {
          context.wrap(() -> super.cancel(message, throwable));
        }

        @Override
        public void halfClose() {
          context.wrap(super::halfClose).run();
        }

        @Override
        public void sendMessage(ReqT message) {
          context.wrap(() -> super.sendMessage(message)).run();
        }

        @Override
        public void setMessageCompression(boolean enabled) {
          context.wrap(() -> super.setMessageCompression(enabled));
        }

        @Override
        public boolean isReady() {
          try {
            return context.wrap(super::isReady).call();
          } catch (Exception e) {
            throw Throwables.propagate(e);
          }
        }
      };
    }
  }

  private static class InboundContextualizedClientInterceptor implements ClientInterceptor {
    private final Context context;

    private InboundContextualizedClientInterceptor(Context context) {
      this.context = context;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          super.start(new ContextualizedClientCallListener<>(responseListener, context), headers);
        }
      };
    }

    private static class ContextualizedClientCallListener<RespT>
        extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {
      private final Context context;

      protected ContextualizedClientCallListener(ClientCall.Listener<RespT> delegate,
          Context context) {
        super(delegate);
        this.context = context;
      }

      @Override
      public void onHeaders(Metadata headers) {
        context.wrap(() -> super.onHeaders(headers)).run();
      }

      @Override
      public void onMessage(RespT message) {
        context.wrap(() -> super.onMessage(message)).run();
      }

      @Override
      public void onClose(Status status, Metadata trailers) {
        context.wrap(() -> super.onClose(status, trailers)).run();
      }

      @Override
      public void onReady() {
        context.wrap(() -> super.onReady());
      }
    }
  }
}
