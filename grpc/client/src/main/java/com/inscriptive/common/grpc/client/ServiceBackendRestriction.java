package com.inscriptive.common.grpc.client;

import com.inscriptive.ns.proto.ServiceDescriptor;

import java.util.Set;

/**
 * Encapsulates logic for picking which service backends to use, out of a given set of possible
 * backends.
 */
@FunctionalInterface
public interface ServiceBackendRestriction {
  Set<ServiceDescriptor> pickFrom(Set<ServiceDescriptor> backends);
}
