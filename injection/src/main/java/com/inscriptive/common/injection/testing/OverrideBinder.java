package com.inscriptive.common.injection.testing;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Provider;

/**
 * Delegates all calls except `install` to the underlying {@link Binder}. The point of this class
 * is to enforce a type-level separation between modules of overrides, i.e. {@link OverrideModule}s,
 * and normal modules.
 *
 * The reason for this is that overrides should be more restricted in scope than ordinary modules.
 * Ordinary modules may have a large tree of other modules which they install. If overrides are
 * allowed to sprawl in a similar way, we can get very confusing unintentional overrides.
 */
public class OverrideBinder {
  private final Binder delegate;

  OverrideBinder(Binder delegate) {
    this.delegate = delegate;
  }

  /**
   * This is key. We don't want to expose a version `install(Module module)`, because that would
   * break the type safety provided by this class.
   */
  public void install(OverrideModule module) {
    module.configure(this);
  }

  /**
   * It is safe to install a provides-method only module since it can only contain bindings that
   * are present in its declared methods, making it very easy to debug.
   */
  public void install(ProvidesMethodModule module) {
    delegate.install(module);
  }

  public <T> LinkedBindingBuilder<T> bind(Key<T> key) {
    return delegate.bind(key);
  }

  public <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> typeLiteral) {
    return delegate.bind(typeLiteral);
  }

  public <T> AnnotatedBindingBuilder<T> bind(Class<T> type) {
    return delegate.bind(type);
  }

  public <T> Provider<T> getProvider(Class<T> klass) {
    return delegate.getProvider(klass);
  }

  public <T> Provider<T> getProvider(Key<T> key) {
    return delegate.getProvider(key);
  }

  /**
   * Deprecated. Use #multibinder() or #mapbinder() instead.
   */
  @Deprecated
  public Binder delegate() {
    return delegate;
  }

  public <T> Multibinder<T> multibinder(Class<T> klass) {
    return Multibinder.newSetBinder(delegate, klass);
  }

  public <K, V> MapBinder<K, V> mapBinder(Class<K> keyClass, Class<V> valueClass) {
    return MapBinder.newMapBinder(delegate, keyClass, valueClass);
  }
}
