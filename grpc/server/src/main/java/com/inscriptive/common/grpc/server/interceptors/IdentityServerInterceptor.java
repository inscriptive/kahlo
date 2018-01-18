package com.inscriptive.common.grpc.server.interceptors;

import com.google.common.base.Throwables;
import com.inscriptive.common.Logger;
import com.inscriptive.common.auth.proto.IdentityPayload;
import com.inscriptive.common.grpc.shared.ContextKeys;
import com.inscriptive.common.grpc.shared.MetadataKeys;
import io.grpc.*;

/**
 * Decodes an identity payload from the headers and saves it in the call {@link Context}.
 */
public class IdentityServerInterceptor implements ServerInterceptor {
  public static final IdentityServerInterceptor INSTANCE = new IdentityServerInterceptor();

  private static final Logger logger = Logger.getLogger(IdentityServerInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

    Context context = Context.current();
    String idToken = metadata.get(MetadataKeys.AUTHENTICATION);
    if (idToken != null) {
      // TODO(shawn) currently only set in foyer, propagate it and make all rpc calls authenticated.
      context = context.withValue(ContextKeys.AUTHENTICATION, idToken);
    }

    // TODO(shawn) don't pass around an identity proto that can be spoofed.
    // Handlers should use jwt claims to check for authorized permissions.
    IdentityPayload payload = null;
    try {
      payload = metadata.get(MetadataKeys.IDENTITY);
    } catch (Exception e) {
      logger.warn("exception decoding identity payload from headers: %s",
          Throwables.getStackTraceAsString(e));
    }
    if (payload == null) {
      logger.debug("could not decode identity payload from headers");
      // TODO(jack / shawn) fail the call with UNAUTHENTICATED (make sure we're propagating auth
      // crednetials everywhere first)
    } else {
      // TODO(jack) save and trace some token from the identity payload
      logger.debug("identity payload size: %d", payload.getSerializedSize());
    }

    context = context.withValue(ContextKeys.IDENTITY, payload);
    return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
  }

  private IdentityServerInterceptor() {
  }
}
