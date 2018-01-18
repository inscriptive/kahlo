package com.inscriptive.common.grpc.server.handlers;

import com.inscriptive.common.exceptions.*;
import io.grpc.Status;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class HandlersExceptionInterceptor implements MethodInterceptor {
  public static final HandlersExceptionInterceptor INSTANCE = new HandlersExceptionInterceptor();

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    try {
      return methodInvocation.proceed();
    } catch (PermissionDeniedException e) {
      throw convertStatus(Status.PERMISSION_DENIED, e);
    } catch (UnauthenticatedException e) {
      throw convertStatus(Status.UNAUTHENTICATED, e);
    } catch (EntityNotFoundException e) {
      throw convertStatus(Status.NOT_FOUND, e);
    } catch (NotImplementedException e) {
      throw convertStatus(Status.UNIMPLEMENTED, e);
    } catch (AlreadyExistsException e) {
      throw convertStatus(Status.ALREADY_EXISTS, e);
    }
  }

  private static RuntimeException convertStatus(Status status, Exception cause) {
    return status.withCause(cause).withDescription(cause.getMessage()).asRuntimeException();
  }
}
