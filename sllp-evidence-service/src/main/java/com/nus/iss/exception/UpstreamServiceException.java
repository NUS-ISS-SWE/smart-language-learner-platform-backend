package com.nus.iss.exception;

/** A dependency this service needs (composition/admin/user-service) could not be reached. */
public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
