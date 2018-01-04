package com.inscriptive.common.grpc.client;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.inscriptive.common.lifecycle.Resource;
import io.grpc.Channel;

import javax.inject.Provider;
import java.util.function.Function;

public class GrpcStubModule<StubT> extends AbstractModule {
  private final String serviceName;
  private final Class<StubT> stubClass;
  private final Function<Channel, StubT> stubMaker;

  public GrpcStubModule(String serviceName, Class<StubT> stubClass,
      Function<Channel, StubT> stubMaker) {
    this.serviceName = serviceName;
    this.stubClass = stubClass;
    this.stubMaker = stubMaker;
  }

  @Override
  protected void configure() {
    Key<Resource<Channel>> channelKey = Key.get(
        new TypeLiteral<Resource<Channel>>() {}, Names.named(serviceName));
    Provider<Resource<Channel>> channelProvider = getProvider(channelKey);
    @SuppressWarnings("unchecked")
    TypeLiteral<Resource<StubT>> stubResourceType = (TypeLiteral<Resource<StubT>>)
        TypeLiteral.get(Types.newParameterizedType(Resource.class, stubClass));
    bind(Key.get(stubResourceType))
        .toInstance(() -> stubMaker.apply(channelProvider.get().get()));
  }
}
