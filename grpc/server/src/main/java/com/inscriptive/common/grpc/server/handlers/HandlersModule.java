package com.inscriptive.common.grpc.server.handlers;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class HandlersModule extends AbstractModule {
  @Override
  protected void configure() {
    bindInterceptor(
        Matchers.subclassesOf(Handler.class),
        Matchers.annotatedWith(Rpc.class),
        HandlersExceptionInterceptor.INSTANCE);
  }
}
