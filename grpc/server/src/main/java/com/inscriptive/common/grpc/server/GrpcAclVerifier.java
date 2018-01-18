package com.inscriptive.common.grpc.server;

import com.inscriptive.acl.proto.BasicPermission;
import com.inscriptive.common.acl.Acl;
import com.inscriptive.common.acl.Permission;

/**
 * Convenience utility for checking an acl against the current user in a grpc handler.
 */
public interface GrpcAclVerifier {
  /**
   * Throws a Status.PERMISSION_DENIED RuntimeException if the current user
   * doesn't have the required permissions.
   */
  void checkRequestAllowed(Acl acl, Permission requiredPermission);

  /**
   * Syntactic sugar for `checkRequestAllow(acl, requiredPermission);
   */
  default void checkRequestAllowed(Acl acl, BasicPermission first, BasicPermission... rest) {
    checkRequestAllowed(acl, Permission.of(first, rest));
  }
}
