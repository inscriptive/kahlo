package com.inscriptive.common.injection;

import com.google.inject.Injector;

public interface Component {
  /**
   * Not for public consumption. Exists to let other interfaces extend this one and use this in
   * default method implementations.
   */
  Injector injector();
}
