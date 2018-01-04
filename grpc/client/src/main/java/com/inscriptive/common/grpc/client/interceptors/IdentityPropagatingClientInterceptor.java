package com.inscriptive.common.grpc.client.interceptors;

import com.inscriptive.common.Logger;
import com.inscriptive.common.auth.proto.IdentityPayload;
import com.inscriptive.common.grpc.shared.ContextKeys;
import io.grpc.*;

import static com.inscriptive.common.grpc.shared.MetadataKeys.IDENTITY;

/**
 * Takes the identity payload from the current {@link Context} and sends it as a call header.
 */
public class IdentityPropagatingClientInterceptor implements ClientInterceptor {
  public static final IdentityPropagatingClientInterceptor
      INSTANCE = new IdentityPropagatingClientInterceptor();

  private static final Logger logger = Logger.getLogger(IdentityPropagatingClientInterceptor.class);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
        channel.newCall(methodDescriptor, callOptions)) {
      @Override
      protected void checkedStart(Listener<RespT> listener, Metadata metadata) throws Exception {
        // TODO(shawn) propagate authentication
        IdentityPayload identityPayload = ContextKeys.IDENTITY.get();
        if (identityPayload != null) {
          metadata.put(IDENTITY, identityPayload);
        }
        delegate().start(listener, metadata);
      }
    };
  }

  private IdentityPropagatingClientInterceptor() {}
}
