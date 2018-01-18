package com.inscriptive.kahlo.grpc.ns;

import org.immutables.value.Value;

@Value.Immutable
public interface ServiceDescriptor {
  String name();
  String id();
  String addr();
  int port();
  State state();

  enum State {
    UNKNOWN,
    UNHEALTHY,
    DRAINING,
    PILOT,
    HEALTHY
  }
}
