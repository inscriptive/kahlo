package com.inscriptive.kahlo.util.net;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

@Value.Immutable(builder = false)
public abstract class Port {
  @Value.Parameter
  public abstract int value();

  @Value.Check
  protected void check() {
    checkArgument(0 < value() && value() < (1 << 16), "Invalid port value: %s", value());
  }

  public static Port of(int value) {
    return ImmutablePort.of(value);
  }
}
