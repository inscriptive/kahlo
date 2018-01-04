package com.inscriptive.common.grpc.client;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A StreamObserver that gives access to a CompletableFuture.
 */
public class FutureStreamObserver<T> implements io.grpc.stub.StreamObserver<T> {
  private final CompletableFuture<T> future = new CompletableFuture<T>();
  private T value;

  /**
   * The CompletableFuture that will contain the response.
   */
  public CompletableFuture<T> completableFuture() {
    return future;
  }

  @Override
  public void onNext(T value) {
    checkNotNull(value, "value");
    checkState(this.value == null, "on next called more than once - streaming response?");
    this.value = value;
  }

  @Override
  public void onError(Throwable throwable) {
    future.completeExceptionally(throwable);
  }

  @Override
  public void onCompleted() {
    checkState(value != null);
    future.complete(value);
  }
}
