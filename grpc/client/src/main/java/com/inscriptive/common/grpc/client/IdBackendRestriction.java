package com.inscriptive.common.grpc.client;

import com.inscriptive.common.Logger;
import com.inscriptive.ns.proto.ServiceDescriptor;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A service backend restriction that picks the service descriptor with the given id.
 */
public class IdBackendRestriction implements ServiceBackendRestriction {
  private static final Logger logger = Logger.getLogger(IdBackendRestriction.class);

  private final String id;

  public IdBackendRestriction(String id) {
    this.id = checkNotNull(id, "id");
  }

  @Override
  public Set<ServiceDescriptor> pickFrom(Set<ServiceDescriptor> backends) {
    logger.info("Picking from backends: %s", backends);
    Set<ServiceDescriptor> matching = backends.stream()
        .filter(sd -> id.equals(sd.getId()))
        .collect(Collectors.toSet());
    if (matching.size() > 1) {
      logger.warn("Multiple service descriptors found with the same id: %s", matching);
    }
    return matching;
  }
}
