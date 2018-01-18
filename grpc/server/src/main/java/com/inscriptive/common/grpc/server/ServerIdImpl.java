package com.inscriptive.common.grpc.server;

import java.lang.annotation.Annotation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * We're copying Guice's NamedImpl class
 */
public class ServerIdImpl implements ServerId {
  private final String serviceName;

  public ServerIdImpl(String serviceName) {
    this.serviceName = checkNotNull(serviceName, "serviceName");
  }

  @Override
  public String serviceName() {
    return serviceName;
  }

  @Override
  public int hashCode() {
    // This is specified in java.lang.Annotation.
    return (127 * "value".hashCode()) ^ serviceName.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServerId)) {
      return false;
    }

    ServerId other = (ServerId) o;
    return serviceName.equals(other.serviceName());
  }

  @Override
  public String toString() {
    return "@" + ServerId.class.getName() + "(serviceName=" + serviceName + ")";
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return ServerId.class;
  }
}
