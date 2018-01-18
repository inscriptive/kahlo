package com.inscriptive.common.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;

import javax.inject.Provider;
import java.util.Collection;
import java.util.function.Function;

/**
 * A description of how to bind a gRPC service descriptor to an implementation of that service.
 */
public class ServerServiceDefinitionRecipe<ImplT extends ServiceT, ServiceT> {
  private final ServiceDescriptor descriptor;
  private final Provider<ImplT> implProvider;
  private final Class<ImplT> implType;
  private final Class<ServiceT> interfaceType;
  private final Function<ServiceT, ServerServiceDefinition> serviceBinder;

  public String fullyQualifiedServiceName() {
    return descriptor.getName();
  }

  public Collection<MethodDescriptor<?, ?>> methods() {
    return descriptor.getMethods();
  }

  public String handlerClassName() {
    return implType.getName();
  }

  private ServerServiceDefinitionRecipe(
      ServiceDescriptor descriptor,
      Provider<ImplT> implProvider,
      Class<ImplT> implType,
      Class<ServiceT> interfaceType,
      Function<ServiceT, ServerServiceDefinition> serviceBinder) {
    // TODO(shawn) run time type check that implType actually matches the descriptor
    // or get grpc to put marker interfaces on shit.

    this.descriptor = descriptor;
    this.implProvider = implProvider;
    this.implType = implType;
    this.interfaceType = interfaceType;
    this.serviceBinder = serviceBinder;
  }

  public static <ImplT extends BindableService> ServerServiceDefinitionRecipe<ImplT,
      BindableService> of(
      ServiceDescriptor descriptor, Provider<ImplT> implProvider, Class<ImplT> implType) {
    return new ServerServiceDefinitionRecipe<>(descriptor, implProvider, implType,
        BindableService.class, BindableService::bindService);
  }

  /**
   * Package private factory method useful in guice module configuration.
   */
  @VisibleForTesting
  public ServerServiceDefinition toServerServiceDefinition() {
    return serviceBinder
        .apply(GrpcGuiceHelpers.shimWithProvider(implProvider, implType, interfaceType));
  }
}
