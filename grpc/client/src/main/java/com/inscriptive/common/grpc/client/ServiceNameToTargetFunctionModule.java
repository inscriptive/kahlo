package com.inscriptive.common.grpc.client;

import com.inscriptive.common.injection.AbstractPerEnvModule;

public class ServiceNameToTargetFunctionModule extends AbstractPerEnvModule {
  @Override
  public void configureCommon() {
  }

  @Override
  public void configureDev() {
    bind(ServiceNameToTargetFunction.class)
        .toInstance(serviceName -> String.format("ns://%s", serviceName));
  }

  @Override
  public void configureCircleCi() {
    configureDev();
  }

  @Override
  public void configureProd() {
    /**
     * see {@link io.grpc.internal.DnsNameResolverProvider}
     */
    bind(ServiceNameToTargetFunction.class)
        .toInstance(serviceName -> String.format("dns:///%s:8080/", serviceName));
  }
}
