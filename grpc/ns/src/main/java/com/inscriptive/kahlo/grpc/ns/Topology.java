package com.inscriptive.kahlo.grpc.ns;

import com.google.common.collect.ListMultimap;
import org.immutables.value.Value;

@Value.Immutable
public interface Topology {
  ListMultimap<String, ServiceDescriptor> services();
}
