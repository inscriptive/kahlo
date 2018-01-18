package com.inscriptive.common.grpc.server;

public class ServerIds {
  private ServerIds() {}

  public static ServerId idFor(String serviceName) {
    return new ServerIdImpl(serviceName);
  }
}
