package com.inscriptive.common.grpc.server;

import com.inscriptive.common.acl.Acl;
import com.inscriptive.common.acl.AclVerifier;
import com.inscriptive.common.acl.Permission;
import com.inscriptive.common.exceptions.PermissionDeniedException;
import io.grpc.Status;

import javax.inject.Inject;

public class GrpcAclVerifierImpl implements GrpcAclVerifier {
  private final AclVerifier aclVerifier;

  @Inject
  public GrpcAclVerifierImpl(AclVerifier aclVerifier) {
    this.aclVerifier = aclVerifier;
  }

  @Override
  public void checkRequestAllowed(Acl acl, Permission requiredPermission) {
    try {
      aclVerifier.checkAllowed(acl, requiredPermission);
    } catch (PermissionDeniedException e) {
      throw Status.PERMISSION_DENIED
          .withCause(e)
          .withDescription(e.getMessage())
          .asRuntimeException();
    }
  }
}
