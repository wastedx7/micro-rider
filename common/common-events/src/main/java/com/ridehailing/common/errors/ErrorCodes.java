package com.ridehailing.common.errors;

/** Stable machine-readable application error codes. */
public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String AUTH_INVALID_TOKEN = "AUTH_INVALID_TOKEN";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String RIDE_NOT_FOUND = "RIDE_NOT_FOUND";
    public static final String RIDE_INVALID_STATE = "RIDE_INVALID_STATE";
    public static final String RIDE_ALREADY_CANCELLED = "RIDE_ALREADY_CANCELLED";
    public static final String DRIVER_NOT_AVAILABLE = "DRIVER_NOT_AVAILABLE";
    public static final String ASSIGNMENT_TIMEOUT = "ASSIGNMENT_TIMEOUT";
    public static final String SESSION_NOT_CONNECTED = "SESSION_NOT_CONNECTED";
    public static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
