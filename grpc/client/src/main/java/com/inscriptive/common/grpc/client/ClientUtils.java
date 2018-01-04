package com.inscriptive.common.grpc.client;

import com.google.common.base.Throwables;
import io.grpc.Status;

import java.util.Optional;
import java.util.concurrent.Callable;

public class ClientUtils {
  private ClientUtils() {}

  public static <T> Optional<T> optionalFromNotFound(Callable<T> callable) {
    try {
      return Optional.of(callable.call());
    } catch (Exception e) {
      if (Status.fromThrowable(e).getCode() == Status.Code.NOT_FOUND) {
        return Optional.empty();
      }
      throw Throwables.propagate(e);
    }
  }
}
