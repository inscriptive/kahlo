package com.inscriptive.common.grpc.client;

import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.inscriptive.common.Logger;
import com.inscriptive.common.grpc.client.interceptors.ParentClientInterceptor;
import com.inscriptive.common.grpc.shared.TracingLoggerPlugin;
import com.inscriptive.common.injection.AbstractPerEnvModule;
import com.inscriptive.kahlo.grpc.ns.YamlNSModule;
import com.inscriptive.kahlo.lifecycle.Lifecycle;
import com.inscriptive.kahlo.lifecycle.Resource;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.internal.DnsNameResolverProvider;

import javax.inject.Provider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class GrpcClientModule extends AbstractPerEnvModule {
  /* The name of the service which we are configuring a client for. */
  private final String serviceName;

  /* Whether or not to configure a channel that is encrypted with TLS */
  private final boolean useTLS;

  /* The list of interceptors to install, in addition to the default list */
  private final List<ClientInterceptor> interceptors;

  private final boolean inProcess;

  public GrpcClientModule(String serviceName, ClientInterceptor... interceptors) {
    this(serviceName, false, interceptors);
  }

  public GrpcClientModule(String serviceName, boolean inProcess,
      ClientInterceptor... interceptors) {
    this(serviceName, false, inProcess, interceptors);
  }

  public GrpcClientModule(String serviceName, boolean useTLS, boolean inProcess,
      ClientInterceptor... interceptors) {
    this.serviceName = serviceName;
    this.useTLS = useTLS;
    this.inProcess = inProcess;
    this.interceptors = Arrays.asList(interceptors);
  }

  @Override
  public void configureCommon() {
    install(new ServiceNameToTargetFunctionModule());
    GrpcChannelModule channelModule = new GrpcChannelModule(inProcess, serviceName, useTLS);
    install(channelModule);

    install(new FactoryModuleBuilder().build(GrpcChannelResource.Factory.class));
    Provider<GrpcChannelResource.Factory> channelResourceFactory = getProvider(
        GrpcChannelResource.Factory.class);

    Key<GrpcChannelResource> namedGrpcChannelResource =
        Key.get(new TypeLiteral<GrpcChannelResource>() {}, Names.named(serviceName));
    Provider<ManagedChannel> uninterceptedChannelProvider = getProvider(channelModule.channelKey());
    bind(namedGrpcChannelResource)
        .toProvider(() -> channelResourceFactory.get().create(
            uninterceptedChannelProvider.get(), serviceName, useTLS, inProcess))
        .in(Scopes.SINGLETON);

    // Bind the lifecycle
    Lifecycle.bindLifecycle(binder())
        .annotatedWith(Names.named(serviceName))
        .to(namedGrpcChannelResource);

    /* we can't reuse the provider that was defined inline above, because binding it in
     * Scopes.SINGLETON _decorates_ it and makes it a singleton. This provider is that singleton
     * provider, which we want to use everywhere.
     */
    Provider<GrpcChannelResource> grpcChannelResourceProvider =
        getProvider(namedGrpcChannelResource);
    // Apply interceptors to the channel
    Provider<Resource<ManagedChannel>> channelProvider = () -> new InterceptedChannelResource(
        grpcChannelResourceProvider.get(), interceptors);

    // Bind @Named(serviceName) Resource<Channel>
    Key<Resource<Channel>> resourceOfChannel =
        Key.get(new TypeLiteral<Resource<Channel>>() {}, Names.named(serviceName));
    bind(resourceOfChannel)
        .toProvider(() -> () -> channelProvider.get().get())
        .in(Scopes.SINGLETON);
    // Bind @Named(serviceName) Resource<ManagedChannel>
    Key<Resource<ManagedChannel>> resourceOfManagedChannel =
        Key.get(new TypeLiteral<Resource<ManagedChannel>>() {}, Names.named(serviceName));
    bind(resourceOfManagedChannel)
        .toProvider(channelProvider)
        .in(Scopes.SINGLETON);

    Logger.registerThreadContextPlugin(TracingLoggerPlugin.INSTANCE);
    Logger.registerThreadContextPlugin(ClientTimingLoggerPlugin.INSTANCE);

    bind(ServiceBackendRestriction.class).to(HealthyBackendRestriction.class).in(Scopes.SINGLETON);
  }

  @Override
  public void configureProd() {
    bind(NameResolver.Factory.class).to(DnsNameResolverProvider.class);
  }

  @Override
  public void configureDev() {
    install(new YamlNSModule());
    bind(Duration.class).annotatedWith(NsSync.class).toInstance(Duration.ofSeconds(1));
    bind(NameResolver.Factory.class).to(NsNameResolverFactory.class);
  }

  @Override
  public void configureCircleCi() {
    configureDev();
  }

  private static class InterceptedChannelResource implements Resource<ManagedChannel> {
    private final Resource<ManagedChannel> channelResource;
    private final List<ClientInterceptor> interceptors;

    private InterceptedChannelResource(Resource<ManagedChannel> channelResource,
        List<ClientInterceptor> interceptors) {
      this.channelResource = channelResource;
      this.interceptors = interceptors;
    }

    @Override
    public ManagedChannel get() {
      return new InterceptorManagedChannel(channelResource.get(),
          new ParentClientInterceptor(interceptors));
    }
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface NsSync {}
}
