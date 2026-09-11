package com.ridehailing.common.auth;

/** Canonical JWT role values. */
public final class AuthRoles {
    private AuthRoles() {
    }

    public static final String RIDER = "RIDER";
    public static final String DRIVER = "DRIVER";
    public static final String ADMIN = "ADMIN";
    public static final String SYSTEM = "SYSTEM";
}
