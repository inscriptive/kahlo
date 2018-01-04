package com.inscriptive.common.grpc.shared;

import com.google.common.base.Stopwatch;
import com.inscriptive.common.auth.proto.IdentityPayload;
import io.grpc.Context;

/**
 * Static instances of {@link Context.Key}. Keys are compared using instance equality, NOT by name.
 */
public class ContextKeys {
  private ContextKeys() {}

  public static final Context.Key<String> TRACE_ID = Context.key("trace_id");

  public static final Context.Key<Stopwatch> SERVER_STOPWATCH = Context.key("server_stopwatch");

  public static final Context.Key<Stopwatch> CLIENT_STOPWATCH = Context.key("client_stopwatch");

  public static final Context.Key<String> AUTHENTICATION = Context.key("authentication");

  public static final Context.Key<IdentityPayload> IDENTITY = Context.key("identity");
}
