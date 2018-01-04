package com.inscriptive.common.grpc.client;

import com.google.common.base.Preconditions;
import com.google.inject.assistedinject.Assisted;
import com.inscriptive.common.Logger;
import com.inscriptive.kahlo.lifecycle.LifecycleResource;
import io.grpc.ManagedChannel;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GrpcChannelResource extends LifecycleResource<ManagedChannel> {
  private static final Logger logger = Logger.getLogger(GrpcChannelResource.class);

  private final ManagedChannel channel;
  private final ExecutorService channelExecutor;
  private final String serviceName;

  @Inject
  public GrpcChannelResource(
      @Assisted ManagedChannel channel,
      @Assisted String serviceName) {
    this.channel = channel;
    // TODO(jack) manage shared grpc client resources, this should be one.
    this.channelExecutor = Executors.newFixedThreadPool(4);
    this.serviceName = serviceName;
  }

  @Override
  public void prepare() {
    resource.set(channel);
  }

  @Override
  public void shutdown() {
    logger.info("Shutting down channels for serviceName: " + serviceName);
    channelExecutor.shutdown();
    ManagedChannel channel = resource.get();
    channel.shutdown();
    try {
      Preconditions.checkArgument(channel.awaitTermination(500, TimeUnit.MILLISECONDS),
          "gRPC channel didn't shut down");
      logger.info("gRPC channel successfully shut down");
    } catch (Exception e) {
      logger.error("Problem shutting down gRPC channel: %s", e);
    } finally {
      channelExecutor.shutdownNow();
      channel.shutdownNow();
    }
  }

  @Override
  public String notProvisionedDescription() {
    return "No healthy connection to serviceName: " + serviceName;
  }

  public interface Factory {
    GrpcChannelResource create(
        @Assisted ManagedChannel channel,
        @Assisted String serviceName, @Assisted("useTLS") boolean useTLS,
        @Assisted("inProcess") boolean inProcess);
  }
}
