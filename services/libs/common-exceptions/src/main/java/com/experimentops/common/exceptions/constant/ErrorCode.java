package com.experimentops.common.exceptions.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {
    GENERAL_ERROR(-1, "Something went wrong", true),
    INVALID_INPUTS(10001, "Invalid input"),
    REQUIRED_FIELD_MISSING(10002, "Required field missing"),
    RESOURCE_NOT_FOUND(10003, "Resource not found"),
    ENTITY_NOT_FOUND(10004, "Entity not found"),
    ENTITY_ALREADY_EXISTS(10005, "Entity already exists"),
    WORKER_CALL_FAILED(20001, "Worker call failed", true),
    WORKER_UNAVAILABLE(20002, "Worker unavailable", true),
    PYTHON_WORKER_ERROR(20003, "Python worker error", true),
    AUTHORIZATION_ERROR(1003, "Access denied", false);

    private final int code;
    private final String message;
    private final boolean serverError;

    ErrorCode(int code, String message) {
        this(code, message, false);
    }

    ErrorCode(int code, String message, boolean serverError) {
        this.code = code;
        this.message = message;
        this.serverError = serverError;
    }
}