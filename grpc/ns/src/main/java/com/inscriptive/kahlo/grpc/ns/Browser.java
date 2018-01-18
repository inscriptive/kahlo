package com.inscriptive.kahlo.grpc.ns;

import java.util.Map;
import java.util.Set;

/**
 * Browser discovers services - the read-only portion of the ns API.
 *
 * xxx not really sure of the best API at all at this point.
 * start simple with a basic sync fetch api.
 *
 * We'll probably want async notification of changes services.
 */
public interface Browser {

  /**
   * Synchronously find all service descriptors by name.
   *
   * @param name the service name, '/' not allowed.
   * @return the set of service descriptors, possibly the empty set.
   */
  Set<ServiceDescriptor> find(String name);

  /**
   * TODO(shawn) implement this in a paginated / efficient way.
   * @return
   */
  Map<String, Set<ServiceDescriptor>> all();
}
