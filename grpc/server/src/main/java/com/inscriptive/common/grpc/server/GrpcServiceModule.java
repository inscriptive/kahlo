package com.inscriptive.common.grpc.server;

import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.inscriptive.common.Logger;
import com.inscriptive.common.grpc.server.handlers.HandlersModule;
import com.inscriptive.common.grpc.server.interceptors.*;
import com.inscriptive.common.grpc.shared.AuthLoggerPlugin;
import com.inscriptive.common.grpc.shared.TracingLoggerPlugin;
import com.inscriptive.common.injection.AbstractPerEnvModule;
import com.inscriptive.common.net.ImmutablePort;
import com.inscriptive.common.util.UUID;
import com.inscriptive.kahlo.util.net.Port;
import com.inscriptive.ns.yaml.YamlNSModule;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.internal.AbstractServerImplBuilder;

import javax.inject.Provider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class GrpcServiceModule extends AbstractPerEnvModule {
  private static final String PRIVATE_IPV4 = "PRIVATE_IPV4";
  private static final String INSCRIPTIVE_POD_NAME = "INSCRIPTIVE_POD_NAME";
  private static final Logger logger = Logger.getLogger(GrpcServiceModule.class);
  // gian said so
  private static final int PREFERRED_EXECUTOR_THREAD_COUNT =
      Runtime.getRuntime().availableProcessors() - 1;

  private final int devPort;
  private final boolean inProcess;
  private final String name;
  private final Collection<ServerServiceDefinitionRecipe<?, ?>> recipes;

  // TODO(shawn) the name should come from config that varies between test and dev.
  public GrpcServiceModule(int devPort, String name,
      Collection<ServerServiceDefinitionRecipe<?, ?>> recipes) {
    this(devPort, false, name, recipes);
  }

  // TODO(shawn) the name should come from config that varies between test and dev.
  public GrpcServiceModule(int devPort, boolean inProcess, String name,
      Collection<ServerServiceDefinitionRecipe<?, ?>> recipes) {
    this.devPort = devPort;
    this.inProcess = inProcess;
    this.name = name;
    this.recipes = recipes;
  }

  @Override
  public void configureCommon() {
    GrpcServerModule serverModule = new GrpcServerModule(name, inProcess);
    install(serverModule);

    Provider<String> addrProvider = getProvider(Key.get(String.class, Addr.class));
    Provider<Port> portProvider =
        getProvider(Key.get(new TypeLiteral<Port>() {}, Names.named(portKey())));

    Provider<MetricsServerInterceptor> metricsServerInterceptorProvider =
        getProvider(MetricsServerInterceptor.class);

    bind(serviceDefinitions())
        .toProvider(serviceDefinitionsProvider(metricsServerInterceptorProvider));

    Provider<Server> serverProvider = serverImplProvider(
        getProvider(serverModule.serverBuilderKey()),
        getProvider(serviceDefinitions()),
        portProvider
    );
    bind(new TypeLiteral<Server>() {})
        .annotatedWith(Names.named(name))
        .toProvider(serverProvider)
        .in(Scopes.SINGLETON);

    Key<GrpcServerLifecycle> namedGrpcServerLifecycle = Key.get(GrpcServerLifecycle.class,
        Names.named(name));
    Provider<String> serverIdProvider = getProvider(Key.get(String.class, ServerIds.idFor(name)));
    bind(namedGrpcServerLifecycle)
        .toProvider(componentProvider(addrProvider, portProvider, serverIdProvider, serverProvider,
            getProvider(serverModule.executorKey())))
        .in(Scopes.SINGLETON);

    Lifecycle.bindLifecycle(binder())
        .annotatedWith(Names.named(name))
        .to(namedGrpcServerLifecycle);

    Logger.registerThreadContextPlugin(TracingLoggerPlugin.INSTANCE);
    Logger.registerThreadContextPlugin(ServerTimingLoggerPlugin.INSTANCE);
    Logger.registerThreadContextPlugin(AuthLoggerPlugin.INSTANCE);

    install(new HandlersModule());
  }

  @Override
  public void configureDev() {
    install(new YamlNSModule());
    bind(Port.class).annotatedWith(Names.named(portKey())).toInstance(ImmutablePort.of(devPort));
    bind(String.class).annotatedWith(Addr.class).toInstance(NSHacks.getAddr());
    bind(String.class).annotatedWith(ServerIds.idFor(name)).toInstance(UUID.get().toString());
  }

  @Override
  public void configureCircleCi() {
    configureDev();
  }

  @Override
  public void configureProd() {
    checkArgument(System.getenv().containsKey(PRIVATE_IPV4),
        "PRIVATE_IPV4 env var not set");
    logger.info("PRIVATE_IPV4=[%s]", System.getenv(PRIVATE_IPV4));
    bind(String.class).annotatedWith(Addr.class).toInstance(System.getenv(PRIVATE_IPV4));
    // always use devPort 8080 in prod
    bind(Port.class).annotatedWith(Names.named(portKey())).toInstance(Port.of(8080));
    checkArgument(System.getenv().containsKey(INSCRIPTIVE_POD_NAME),
        "INSCRIPTIVE_POD_NAME env var not set");
    bind(String.class).annotatedWith(ServerIds.idFor(name))
        .toInstance(System.getenv(INSCRIPTIVE_POD_NAME));
  }

  /**
   * The ServerServiceDefinition for the registered services.
   *
   * All interceptors have been applied.
   * The ServerMethodDefinition's handlers are guice-injected on each call.
   *
   * Exposed as a provider to allow advanced proxy-ing of calls in-proc.
   *
   * Bind as a singleton because we do reflection under the hood.
   * Rcp dispatches would become very slow if it's not a singleton.
   *
   * @see GrpcGuiceHelpers
   * @see ServerServiceDefinition
   * @see ServiceDescriptor
   * @see io.grpc.ServerMethodDefinition
   */
  public Provider<Collection<ServerServiceDefinition>> serviceDefinitionsProvider(
      Provider<MetricsServerInterceptor> metricsServerInterceptorProvider) {
    return () -> recipes.stream()
        .map(ServerServiceDefinitionRecipe::toServerServiceDefinition)
        .map(def -> applyInterceptors(def, metricsServerInterceptorProvider))
        .collect(Collectors.toList());
  }

  private String portKey() {
    // TODO(shawn) name shouldn't be used in the devPort picking
    return "grpc_server:" + name;
  }

  private Provider<Server> serverImplProvider(
      Provider<AbstractServerImplBuilder> serverBuilderProvider,
      Provider<Collection<ServerServiceDefinition>> serverServiceDefinition,
      Provider<Port> portProvider) {
    return () -> {
      int port = portProvider.get().value();
      logger.debug("gRPC server provided port=%d", port);
      AbstractServerImplBuilder serverBuilder = serverBuilderProvider.get();
      serverServiceDefinition.get().forEach(serverBuilder::addService);
      return serverBuilder.build();
    };
  }

  private Provider<GrpcServerLifecycle> componentProvider(
      Provider<String> addrProvider,
      Provider<Port> portProvider,
      Provider<String> serverIdProvider,
      Provider<Server> serverProvider,
      Provider<ExecutorService> executorServiceProvider) {
    return () -> new GrpcServerLifecycle(addrProvider.get(), portProvider.get(),
        serverProvider.get(), recipes, serverIdProvider.get(),
        executorServiceProvider.get());
  }

  private ServerServiceDefinition applyInterceptors(ServerServiceDefinition delegate,
      Provider<MetricsServerInterceptor> metricsServerInterceptorProvider) {
    return ServerInterceptors.intercept(delegate,
        metricsServerInterceptorProvider.get(),
        LoggingServerInterceptor.INSTANCE,
        IdentityServerInterceptor.INSTANCE,
        TimingServerInterceptor.INSTANCE,
        TracingServerInterceptor.INSTANCE
    );
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Addr {
  }

  private Key<Collection<ServerServiceDefinition>> serviceDefinitions() {
    return Key.get(
        new TypeLiteral<Collection<ServerServiceDefinition>>() {},
        Names.named(name));
  }
}
