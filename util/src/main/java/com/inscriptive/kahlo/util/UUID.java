package com.inscriptive.kahlo.util;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * UUID captures a 144-bit value as a URL-safe string (24 characters with no padding).
 *
 * TODO(shawn) why isn't 128-bit encoded into 22 Base64 characters enough?
 */
public final class UUID {
  private final String representation;
  // Base64 encodes at 6-bits-per-input-byte 144-bits / 6 = 24.
  public static final int EXPECTED_UUID_LENGTH = 24;
  private static final UUIDSupplier UUID_SUPPLIER = new UUIDSupplier();

  private UUID(String representation) {
    checkArgument(representation.length() == EXPECTED_UUID_LENGTH,
        "UUID string: '%s' has wrong length: %s expected %s",
        representation, representation.length(), EXPECTED_UUID_LENGTH);
    checkArgument(representation.matches("[a-zA-Z_]+"), "Invalid token: %s", representation);

    this.representation = representation;
  }

  public String toString() {
    return representation;
  }

  public static UUID of(String uuid) {
    return new UUID(uuid);
  }

  public static UUID get() {
    return UUID_SUPPLIER.get();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UUID that = (UUID) o;

    return Objects.equal(this.representation, that.representation);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(representation);
  }
}
