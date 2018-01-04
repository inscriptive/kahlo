package com.inscriptive.kahlo.lifecycle;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

import java.lang.annotation.Annotation;

/**
 * Interface for components that hook into an app's lifecycle.
 *
 * All implementations of a given stage are invoked concurrently.
 */
public interface Lifecycle {

  /**
   * The multibinder that clients of this interface should bind themselves in.
   *
   * E.g. `LifecycleComponent.multibinder(binder()).addBinding(...)`
   *
   * @deprecated prefer other versions because they do not leak the multibinder dependency, which
   * is now explicit with Buck.
   */
  @Deprecated
  static Multibinder<Lifecycle> multibinder(Binder binder) {
    return Multibinder.newSetBinder(binder, Lifecycle.class);
  }

  /**
   * Start a binding builder for a lifecycle
   * @param binder the current `Binder`
   */
  @Deprecated
  static LifecycleBindingBuilder addBinding(Binder binder) {
    return new LifecycleBindingBuilder(binder);
  }

  /**
   * Lifecycles are bound using a multibinder. Guice's multibinders are append-only. That means that
   * instead of overriding a lifecycle binding directly, we need to override a linked lifecycle
   * binding:
   *
   * ```
   * class Module {
   *   Lifecycle.addBinding(binder()).via(Service.class).to(ServiceImpl.class);
   * }
   * class OverrideModule {
   *   Lifecycle.addBinding(binder()).via(Service.class).to(FakeService.class);
   * }
   * ```
   *
   * Note that we are binding the same lifecycle in both the module and its override:
   * `Service.class`. However, we provide different implementations of that interface, which
   * override each other. This `LifecycleBindingBuilder` attempts to enforce this pattern, to make
   * it easier for clients to do the right thing when it comes to lifecycles and override modules.
   *
   * @deprecated :(
   * turns out calling binder.bind(key) leaves a dangling no-op binding, which can lead to very
   * confusing binder errors. Thus, calling .via() without any additional calls to the returned
   * binding builder is dangerous. Use #bindLifecycle instead.
   */
  @Deprecated
  class LifecycleBindingBuilder {
    private final Binder binder;

    public LifecycleBindingBuilder(Binder binder) {
      this.binder = binder;
    }

    /**
     * Bind the given key as a lifecycle. Returns a `LinkedBindingBuilder` for the given key, so
     * that clients can choose the implementation of that key separately.
     * @param key the key to bind as a lifecycle
     * @return a binding builder for the given key
     */
    public <S extends Lifecycle> LinkedBindingBuilder<S> via(Key<S> key) {
      multibinder(binder).addBinding().to(key);
      return binder.bind(key);
    }

    /**
     * Bind the given type as a lifecycle. Returns a `LinkedBindingBuilder` for the given type, so
     * clients can choose the implementation of that type separately.
     * @param klass the type to bind as a lifecycle.
     * @return a binding builder for the given type
     */
    public <S extends Lifecycle> LinkedBindingBuilder<S> via(Class<S> klass) {
      return via(Key.get(klass));
    }
  }

  /*
  Usage:
  Lifecycle.bindLifecycle()
      .annotatedWith(Annotation.class)
      .to(Foo.class);
   */
  class BindingBuilder {
    private final Binder binder;

    public BindingBuilder(Binder binder) {
      this.binder = binder;
    }

    public LinkedBindingBuilder<Lifecycle> annotatedWith(Annotation annotation) {
      return strictBindingBuilder(Key.get(Lifecycle.class, annotation));
    }

    public LinkedBindingBuilder<Lifecycle> annotatedWith(
        Class<? extends Annotation> annotationType) {
      return strictBindingBuilder(Key.get(Lifecycle.class, annotationType));
    }

    private LinkedBindingBuilder<Lifecycle> strictBindingBuilder(Key<Lifecycle> key) {
      multibinder(binder).addBinding().to(key);
      // require a binding to the key
      binder.getProvider(key);
      return binder.bind(key);
    }
  }

  static BindingBuilder bindLifecycle(Binder binder) {
    return new BindingBuilder(binder);
  }

  static void bindLifecycleToNoOp(Binder binder, Annotation annotation) {
    bindLifecycle(binder).annotatedWith(annotation).to(NoOpLifecycle.class);
  }

  static void bindLifecycleToNoOp(Binder binder, Class<? extends Annotation> annotationType) {
    bindLifecycle(binder).annotatedWith(annotationType).to(NoOpLifecycle.class);
  }

  /*
   * Methods are defined in order of their execution
   *
   */

  /**
   * Prepare any internal state.
   *
   * Read configs and initialize any state but don't try to reach external
   * dependencies (e.g. don't health check data stores or downstream services)
   * since other components are also being prepared.
   *
   * Don't do cross-component things, the world is still being prepared.
   */
  default void prepare() {}

  /**
   * Validate any cross component state and the larger environment you depend on.
   *
   * All components are essentially spun up and ready to start but we want
   * to check everything to fail as early and hard as possible.
   *
   * Examples:
   *
   *   - If you're a data store client, try and make the first connection.
   *
   *   - Check that your various down stream services are healthy.
   *
   *   - If you're a stateful service component, check that your data store schema
   *     matches your expectations.
   *
   * It should be possible (but potentially unsafe) to skip this step.
   */
  default void validate() {}

  /**
   * Start long-running components.
   */
  default void start() {}

  /**
   * Try to clean up as gracefully as possible and exit.
   */
  default void shutdown() {}
}
