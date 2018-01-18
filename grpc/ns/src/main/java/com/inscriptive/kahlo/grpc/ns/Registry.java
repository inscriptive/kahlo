package com.inscriptive.kahlo.grpc.ns;

/**
 * Registry broadcasts service information - the write-only portion of the ns API.
 *
 * xxx not really sure of the best API at all at this point.
 * start simple with a basic sync fetch api.
 *
 * We'll probably want async notification of changes services.
 */
public interface Registry {
  void announce(ServiceDescriptor sd);

  static String serverDescription(String serverId, String addr, int port) {
    // TODO(shawn) create a better server description abstraction
    return String.format("%s (%s:%d)", serverId, addr, port);
  }
}
