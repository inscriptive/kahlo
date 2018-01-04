package com.inscriptive.common.grpc.client;

import io.grpc.*;

import java.util.concurrent.TimeUnit;

/**
 * See {@link ClientInterceptors#intercept}
 */
class InterceptorManagedChannel extends ManagedChannel {
  private final ManagedChannel delegate;
  private final ClientInterceptor interceptor;

  InterceptorManagedChannel(ManagedChannel delegate, ClientInterceptor interceptor) {
    this.delegate = delegate;
    this.interceptor = interceptor;
  }

  @Override
  public ManagedChannel shutdown() {
    return delegate.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public ManagedChannel shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
      MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
    return interceptor.interceptCall(methodDescriptor, callOptions, delegate);
  }

  @Override
  public String authority() {
    return delegate.authority();
  }
}
