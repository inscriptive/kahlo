package com.inscriptive.common.injection.testing;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Module declaring both a base module and some overrides for the base module. It can be installed
 * normally in a test without worrying about `Modules.override` boilerplate. Alternatively, it can
 * be composed with other test modules, which may want to includes its `#overrides` within their
 * `#overrides`.
 */
public interface TestModule extends Module {
  default void configure(Binder binder) {
    binder.install(overrides().applyOverrides(baseModule()));
  }

  Module baseModule();

  OverrideModule overrides();
}
