package com.inscriptive.kahlo.grpc.ns;

import javax.inject.Singleton;

@Singleton
public class YamlRegistry implements Registry {
  @Override
  public void announce(ServiceDescriptor sd) {
    // no-op
  }
}
