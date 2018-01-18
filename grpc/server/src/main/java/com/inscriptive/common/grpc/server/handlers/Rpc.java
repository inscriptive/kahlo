package com.inscriptive.common.grpc.server.handlers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker annotation for methods which perform actual RPC handling.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Rpc {}
