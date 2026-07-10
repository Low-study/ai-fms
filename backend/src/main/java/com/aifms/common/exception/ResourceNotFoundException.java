package com.aifms.common.exception;

import com.aifms.common.ErrorCodes;

/**
 * Thrown when a requested resource is not found.
 * Results in HTTP 404.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCodes.USER_NOT_FOUND, resource + " not found: " + id);
    }
}
