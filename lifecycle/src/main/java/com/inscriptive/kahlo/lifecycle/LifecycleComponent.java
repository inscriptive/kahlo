package com.inscriptive.kahlo.lifecycle;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.inscriptive.common.injection.Component;

import java.util.Set;

public interface LifecycleComponent extends Component {
  /**
   * @return all lifecycles bound with {@link Lifecycle#multibinder(Binder)}
   */
  default Set<? extends Lifecycle> lifecycles() {
    return injector().getInstance(Key.get(new TypeLiteral<Set<Lifecycle>>() { }));
  }

  default LifecycleManager lifecycleManager() {
    return injector().getInstance(LifecycleManager.class);
  }
}
