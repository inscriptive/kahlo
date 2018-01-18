package com.inscriptive.common.grpc.server;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.inscriptive.common.net.Port;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.AbstractServerImplBuilder;
import io.grpc.netty.NettyServerBuilder;

import javax.inject.Provider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrpcServerModule extends AbstractModule {
  private static final int PREFERRED_EXECUTOR_THREAD_COUNT =
      Runtime.getRuntime().availableProcessors() - 1;

  private final String name;
  private final boolean inProcess;

  public GrpcServerModule(String name, boolean inProcess) {
    this.name = name;
    this.inProcess = inProcess;
  }

  @Override
  public void configure() {
    ExecutorService executor = Executors.newFixedThreadPool(
        PREFERRED_EXECUTOR_THREAD_COUNT < 1 ? 1 : PREFERRED_EXECUTOR_THREAD_COUNT);
    bind(ExecutorService.class)
        .annotatedWith(Names.named(name))
        .toInstance(executor);

    Provider<Port> portProvider =
        getProvider(Key.get(new TypeLiteral<Port>() {
        }, Names.named(portKey())));
    Provider<ExecutorService> executorProvider = getProvider(executorKey());
    bind(serverBuilderKey())
        .toProvider(() -> {
          int port = portProvider.get().value();
          AbstractServerImplBuilder serverBuilder = inProcess
              ? InProcessServerBuilder.forName(name)
              : NettyServerBuilder.forPort(port);
          serverBuilder.executor(executorProvider.get());
          return serverBuilder;
        })
        .in(Scopes.SINGLETON);
  }

  public Key<ExecutorService> executorKey() {
    return Key.get(ExecutorService.class, Names.named(name));
  }

  public Key<AbstractServerImplBuilder> serverBuilderKey() {
    return Key.get(AbstractServerImplBuilder.class, Names.named(name));
  }

  private String portKey() {
    // TODO(shawn) name shouldn't be used in the port picking
    return "grpc_server:" + name;
  }
}
