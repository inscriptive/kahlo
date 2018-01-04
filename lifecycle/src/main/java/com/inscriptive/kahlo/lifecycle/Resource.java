package com.inscriptive.kahlo.lifecycle;

/**
 * A {@code Resource} is a handle to some entity.
 *
 * The entity might be indirected via a {@code Resource} for a variety of reasons.
 * For example, the resource might be expensive to create or maintain like a
 * pooled DB connection or the resource might come and go intermittently like
 * an RPC's transport socket to a remote server.
 *
 * A {@code Resource} may or may not participate in the application
 * {@code Lifecycle}.
 *
 * @see LifecycleResource
 *
 * @param <T>
 */
public interface Resource<T> {
  T get();
}
