package com.inscriptive.common.injection.testing;

import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * A collection of override bindings. See {@link TestModule}.
 */
public interface OverrideModule {
  void configure(OverrideBinder binder);

  default Module applyOverrides(Module module) {
    return Modules.override(module)
        .with(binder -> configure(new OverrideBinder(binder)));
  }
}
