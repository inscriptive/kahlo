package com.inscriptive.common.injection.testing;

import com.google.inject.Module;

import java.util.stream.Stream;

public class TestModules {
  private TestModules() {
  }

  public static TestModule combine(TestModule... modules) {
    return new TestModule() {
      @Override
      public Module baseModule() {
        return binder -> Stream.of(modules)
            .map(TestModule::baseModule)
            .forEach(binder::install);
      }

      @Override
      public OverrideModule overrides() {
        return binder -> Stream.of(modules)
            .map(TestModule::overrides)
            .forEach(binder::install);
      }
    };
  }
}
