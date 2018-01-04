package com.inscriptive.common.grpc.client;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.internal.AbstractManagedChannelImplBuilder;
import io.grpc.netty.NettyChannelBuilder;

import javax.inject.Provider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.grpc.netty.NegotiationType.PLAINTEXT;
import static io.grpc.netty.NegotiationType.TLS;

public class GrpcChannelModule extends AbstractModule {
  private final boolean inProcess;
  private final String serviceName;
  private final boolean useTLS;

  public GrpcChannelModule(boolean inProcess, String serviceName, boolean useTLS) {
    this.inProcess = inProcess;
    this.serviceName = serviceName;
    this.useTLS = useTLS;
  }

  @Override
  protected void configure() {
    bind(executorKey())
        .toProvider(() -> Executors.newFixedThreadPool(4))
        .in(Scopes.SINGLETON);
    Provider<ServiceNameToTargetFunction> serviceNameToTargetFunctionProvider =
        getProvider(ServiceNameToTargetFunction.class);
    Provider<ExecutorService> executorProvider = getProvider(executorKey());
    Provider<KetamaHashingLoadBalancerFactory> loadBalancerFactoryProvider =
        getProvider(KetamaHashingLoadBalancerFactory.class);
    Provider<NameResolver.Factory> nameResolverFactoryProvider =
        getProvider(NameResolver.Factory.class);

    bind(channelKey())
        .toProvider(() -> {
          AbstractManagedChannelImplBuilder builder;
          // Hack alert
          if (serviceName.equals("pubsub.googleapis.com")) {
            builder = NettyChannelBuilder.forAddress("pubsub.googleapis.com", 443)
                .negotiationType(TLS);
          } else {
            String target = serviceNameToTargetFunctionProvider.get().apply(serviceName);
            builder = inProcess
                ? InProcessChannelBuilder.forName(target)
                : NettyChannelBuilder.forTarget(target)
                .negotiationType(useTLS ? TLS : PLAINTEXT);
          }
          return builder.executor(executorProvider.get())
              .loadBalancerFactory(loadBalancerFactoryProvider.get())
              .nameResolverFactory(nameResolverFactoryProvider.get())
              .build();
        });
  }

  public Key<ManagedChannel> channelKey() {
    return Key.get(ManagedChannel.class, Names.named(serviceName));
  }

  public Key<ExecutorService> executorKey() {
    return Key.get(ExecutorService.class, Names.named(serviceName));
  }
}
