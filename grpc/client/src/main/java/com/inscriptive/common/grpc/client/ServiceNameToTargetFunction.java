package com.inscriptive.common.grpc.client;

import java.util.function.Function;

public interface ServiceNameToTargetFunction extends Function<String, String> {
}
