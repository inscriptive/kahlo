package com.inscriptive.common.grpc.server;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

class GrpcGuiceHelpers {
  private GrpcGuiceHelpers() {
  }

  /**
   * Returns a proxy impl of type T which invokes the given provider to get a delegate for every
   * method call.
   *
   * @param provider      a provider of implementations for
   * @param impl          an implementing class of the interface
   * @param grpcInterface the interface which the return type should implement
   * @param <S>           the implementation type
   * @param <T>           the interface type
   * @return an implementation of the interface T
   */
  static <S extends T, T> T shimWithProvider(Provider<S> provider, Class<S> impl,
      Class<T> grpcInterface) {
    // cache method handles so we aren't performing reflection on every rpc call
    Map<String, Method> implMethodHandles = implMethodHandles(impl, grpcInterface);

    //noinspection unchecked
    return (T) Proxy.newProxyInstance(
        grpcInterface.getClassLoader(),
        new Class<?>[]{grpcInterface},
        (proxy, method, args) -> {
          S delegate = provider.get();
          Method implMethod = implMethodHandles.get(method.getName());
          return implMethod.invoke(delegate, args);
        });
  }

  private static <S extends T, T> Map<String, Method> implMethodHandles(Class<S> impl,
      Class<T> grpcInterface) {
    ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
    for (Method interfaceMethod : grpcInterface.getDeclaredMethods()) {
      try {
        builder.put(interfaceMethod.getName(),
            impl.getMethod(interfaceMethod.getName(),
                (Class<?>[]) interfaceMethod.getParameterTypes()));
      } catch (NoSuchMethodException e) {
        Throwables.propagate(e);
      }
    }
    return builder.build();
  }
}
