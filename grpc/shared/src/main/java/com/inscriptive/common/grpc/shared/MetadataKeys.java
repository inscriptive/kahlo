package com.inscriptive.common.grpc.shared;

import io.grpc.Metadata;

/**
 * Static instances of keys for {@link Metadata}.
 */
public class MetadataKeys {
  private MetadataKeys() {
  }

  public static final Metadata.Key<String> TRACE_ID =
      Metadata.Key.of("trace_id", Marshallers.SIMPLE_ASCII_MARSHALLER);

  public static final Metadata.Key<String> AUTHENTICATION =
      Metadata.Key.of("authentication", Marshallers.SIMPLE_ASCII_MARSHALLER);
}
