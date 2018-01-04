package com.inscriptive.common.grpc.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.inscriptive.ns.Browser;
import com.inscriptive.ns.proto.ServiceDescriptor;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.ResolvedServerInfo;
import io.grpc.ResolvedServerInfoGroup;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NsNameResolverFactory extends NameResolver.Factory {
  private final Browser browser;
  private final ServiceBackendRestriction serviceBackendRestriction;
  private final Duration syncDuration;

  @Inject
  public NsNameResolverFactory(Browser browser,
      ServiceBackendRestriction serviceBackendRestriction,
      @GrpcClientModule.NsSync Duration syncDuration) {
    this.browser = browser;
    this.serviceBackendRestriction = serviceBackendRestriction;
    this.syncDuration = syncDuration;
  }

  @Nullable
  @Override
  public NameResolver newNameResolver(URI targetUri, Attributes params) {
    String serviceName = targetUri.getAuthority();

    return new NameResolver() {
      private Set<ServiceDescriptor> lastResolvedDescriptors = ImmutableSet.of();
      private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

      @Override
      public String getServiceAuthority() {
        return serviceName;
      }

      @Override
      public void start(Listener listener) {
        syncOnce(listener);
        executor.schedule(() -> {
          syncOnce(listener);
        }, syncDuration.toMillis(), TimeUnit.MILLISECONDS);
      }

      private void syncOnce(Listener listener) {
        Set<ServiceDescriptor> serviceDescriptors = serviceBackendRestriction.pickFrom(
            browser.find(serviceName));
        if (serviceDescriptors.equals(lastResolvedDescriptors)) {
          return;
        }
        ResolvedServerInfoGroup.Builder groupBuilder = ResolvedServerInfoGroup.builder();
        serviceDescriptors.forEach(sd -> groupBuilder.add(new ResolvedServerInfo(
            new InetSocketAddress(sd.getAddr(), sd.getPort()))));
        listener.onUpdate(ImmutableList.of(groupBuilder.build()), Attributes.EMPTY);
      }

      @Override
      public void shutdown() {
        executor.shutdownNow();
      }
    };
  }

  @Override
  public String getDefaultScheme() {
    return "ns";
  }
}
