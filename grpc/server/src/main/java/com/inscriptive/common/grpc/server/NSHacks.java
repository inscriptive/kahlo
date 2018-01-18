package com.inscriptive.common.grpc.server;

import com.google.common.net.InetAddresses;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static com.google.common.base.Throwables.propagate;

// TODO(shawn) make this better (not sure how yet)
// at the very least guard this behind a dev env only check so it doesn't run in prod
public class NSHacks {
  public static final String PRIVATE_IPV4 = "PRIVATE_IPV4";

  public static String getAddr() {
    // We assume the addr is either properly provided in the environment or
    // we're running in DEV on an OS X host.
    String addr = System.getenv().containsKey(PRIVATE_IPV4)
        ? System.getenv().get(PRIVATE_IPV4)
        // lmfao this breaks because I'm not on the internet. (jack)
        : getDevOSXAddr();

    // TODO(shawn) check well formed-ness.
    if (addr == null) {
      throw new RuntimeException("Failed to find reachable inet addr.");
    }

    return addr;
  }

  private static String getDevOSXAddr() {
    Enumeration<InetAddress> addrs;
    try {
      // TODO(shawn) HACK HACK HACK
      addrs = NetworkInterface.getByName("lo0").getInetAddresses();
    } catch (SocketException e) {
      throw propagate(e);
    }
    String addr = null;
    while (addrs.hasMoreElements()) {
      InetAddress i = addrs.nextElement();
      if (i instanceof Inet4Address) {
        addr = InetAddresses.toAddrString(i);
        break;
      }
    }

    return addr;
  }

  private NSHacks() { }
}
