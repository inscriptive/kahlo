package com.inscriptive.kahlo.util;

import java.security.SecureRandom;
import java.util.function.Supplier;

/**
 * Produces 144-bit UUIDs.
 *
 * TODO(shawn) why isn't 128-bit good enough?
 */
public final class UUIDSupplier implements Supplier<UUID> {
  private SecureRandom random;

  public UUIDSupplier() {
    this.random = new SecureRandom();
  }

  @Override
  public UUID get() {
    return UUID.of(RandomStrings.randomString(24));
  }
}
