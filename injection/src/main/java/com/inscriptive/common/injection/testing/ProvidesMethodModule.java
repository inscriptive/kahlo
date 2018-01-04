package com.inscriptive.common.injection.testing;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * A module which is restricted to contain only @Provides methods
 */
public abstract class ProvidesMethodModule implements Module {
  @Override
  public final void configure(Binder binder) {}
}
