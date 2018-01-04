package com.inscriptive.common.grpc.shared;

import io.grpc.Status;

import java.util.function.Supplier;

public class GrpcRequestPreconditions {
  private GrpcRequestPreconditions() {
  }

  public static boolean checkRequest(boolean condition) {
    return checkRequest(condition, "");
  }

  public static boolean checkRequest(boolean condition, String format, Object... args) {
    return checkRequest(condition, Status.INVALID_ARGUMENT, format, args);
  }

  /**
   * Throw the given status as a runtime exception, if the condition fails.
   *
   * The given format string and args will be included as a status description.
   */
  public static boolean checkRequest(boolean condition, Status status, String format,
      Object... args) {
    if (!condition) {
      throw status.withDescription(String.format(format, args)).asRuntimeException();
    }
    return true;
  }

  /**
   * Convert request values into domain objects.
   *
   * IllegalArgumentExceptions result in an an INVALID_ARGUMENT grpc status rather than a server
   * error.
   *
   * e.g.: `MyEntity entity = checkRequest(() -> toMyEntity(request.arg));`
   *
   * @param domainObject produce a non-null domain object.
   * @param <T>      the domain object type.
   * @return the result of domainObject.
   * @throws io.grpc.StatusRuntimeException a gRPC INVALID_ARGUMENT exception if domainObject
   *                                        raises any exception or returns a null value.
   */
  public static <T> T checkRequest(Supplier<T> domainObject) {
    try {
      return domainObject.get();
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT
          .withCause(e)
          .withDescription(e.getMessage())
          .asRuntimeException();
    }
  }
}
