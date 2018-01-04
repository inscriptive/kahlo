package com.inscriptive.kahlo.lifecycle;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@code LifecycleResource} is an atomic reference to an entity that's eventually available.
 *
 * Provides an abstract implementation for a {@code Resource} that also wants to participate in the
 * {@code Lifecycle}.
 *
 * @param <T>
 */
public abstract class LifecycleResource<T> implements Lifecycle, Resource<T> {
  protected final AtomicReference<T> resource;

  protected LifecycleResource() {
    resource = new AtomicReference<>();
  }

  @Override
  public T get() {
    T result = resource.get();
    if (result == null) {
      throw new NotProvisionedException(notProvisionedDescription());
    } else {
      return result;
    }
  }

  protected abstract String notProvisionedDescription();

  /**
   * Indicates that the {@code LifecycleResource} reference isn't set.
   *
   * Provides more meaning in stack traces than a null pointer.
   */
  public static final class NotProvisionedException extends NullPointerException {
    public NotProvisionedException(String message) {
      super(message);
    }
  }
}
