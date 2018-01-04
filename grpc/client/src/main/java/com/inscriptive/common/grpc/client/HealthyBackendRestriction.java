package com.inscriptive.common.grpc.client;

import com.inscriptive.ns.proto.ServiceDescriptor;

import java.util.Set;
import java.util.stream.Collectors;

public class HealthyBackendRestriction implements ServiceBackendRestriction {
  @Override
  public Set<ServiceDescriptor> pickFrom(Set<ServiceDescriptor> backends) {
    return backends.stream()
        .filter(sd -> sd.getState() == ServiceDescriptor.State.HEALTHY)
        .collect(Collectors.toSet());
  }
}
