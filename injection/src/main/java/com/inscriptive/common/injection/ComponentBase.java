package com.inscriptive.common.injection;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Encapsulates an injector. Subclasses should expose public methods that provide entry points
 * into an application.
 */
public abstract class ComponentBase implements Component {
  protected final Injector injector;

  /**
   * Construct a component from another component, whose injection scope we will share
   */
  protected ComponentBase(Component component) {
    this(component.injector());
  }

  /**
   * Construct a component with a new injection scope
   */
  protected ComponentBase(Module module) {
    this(Guice.createInjector(module));
  }

  private ComponentBase(Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
  }

  @Override
  public Injector injector() {
    return injector;
  }
}
