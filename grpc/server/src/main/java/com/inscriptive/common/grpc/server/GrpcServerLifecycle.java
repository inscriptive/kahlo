package com.inscriptive.common.grpc.server;

import com.google.common.base.Preconditions;
import com.inscriptive.common.Logger;
import com.inscriptive.common.lifecycle.Lifecycle;
import com.inscriptive.common.net.Port;
import io.grpc.Server;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.inscriptive.ns.Registry.serverDescription;

public class GrpcServerLifecycle implements Lifecycle {
  private static final Logger logger = Logger.getLogger(GrpcServerLifecycle.class);

  private final String addr;
  private final Port port;
  private final Server server;
  private final Collection<ServerServiceDefinitionRecipe<?, ?>> serviceDefinitions;
  private final String serverId;
  private final ExecutorService executor;


  public GrpcServerLifecycle(String addr, Port port, Server server,
      Collection<ServerServiceDefinitionRecipe<?, ?>> serviceDefinitions,
      String serverId, ExecutorService executor) {
    this.addr = addr;
    this.port = port;
    this.server = server;
    this.serviceDefinitions = serviceDefinitions;
    this.serverId = serverId;
    this.executor = executor;
  }

  @Override
  public void start() {
    try {
      server.start();

      // TODO(shawn) put the serverId in the logger context

      String serverDescription = serverDescription(serverId, addr, port.value());
      logger.info("started gRPC server: %s", serverDescription);
      serviceDefinitions.forEach(def -> {
        logger.info("Service: %s -> %s", def.fullyQualifiedServiceName(), def.handlerClassName());
        def.methods().forEach(method ->
            logger.info("\t%s", method.getFullMethodName()));
      });
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  @Override
  public void shutdown() {
    logger.info("Shutting down gRPC server");
    server.shutdown();
    executor.shutdown();
    try {
      Preconditions.checkArgument(server.awaitTermination(500, TimeUnit.MILLISECONDS),
          "gRPC didn't shut down");
      logger.info("gRPC successfully shut down");
    } catch (Exception e) {
      logger.error("Problem shutting down gRPC server: %s", e);
    } finally {
      executor.shutdownNow();
    }
  }
}
