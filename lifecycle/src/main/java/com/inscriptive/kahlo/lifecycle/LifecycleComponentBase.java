package com.inscriptive.kahlo.lifecycle;

import com.google.inject.Module;
import com.inscriptive.common.injection.Component;
import com.inscriptive.common.injection.ComponentBase;

public class LifecycleComponentBase extends ComponentBase implements LifecycleComponent {
  public LifecycleComponentBase(Module module) {
    super(module);
  }

  public LifecycleComponentBase(Component component) {
    super(component);
  }
}
