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
    WORKSPACE_NOT_FOUND(10006, "Workspace not found", false),
    USER_NOT_FOUND(10007, "User not found", false),
    TOKEN_NOT_FOUND(10008, "Token Not Found. Please try again"),
    RESET_PASSWORD_TOKEN_EXPIRED(10009, "Session expired. Please refresh and try again."),
    EMAIL_OR_USERNAME_ALREADY_EXISTS(10014, "Email or username already exists", false),
    ROLE_CREATION_FAILED(10015, "Role creation failed", true),
    WORKER_CALL_FAILED(20001, "Worker call failed", true),
    WORKER_UNAVAILABLE(20002, "Worker unavailable", true),
    PYTHON_WORKER_ERROR(20003, "Python worker error", true),
    AUTHORIZATION_ERROR(10013, "Access denied", false),
    KEYCLOAK_ACCOUNT_NOT_FULLY_SETUP(30001,"Account is not fully set up", false),
    KEYCLOAK_TOKEN_INVALID_CREDENTIALS(30002, "Invalid username or password", true),
    KEYCLOAK_TOKEN_ERROR(30003, "Unable to login", false),
    KEYCLOAK_USER_NOT_FOUND(30004, "User Not found in KeyCloak.", true),
    KEYCLOAK_PASSWORD_VALIDATION(30005, "Invalid password. Please make sure your password is strong and hasn't been used before.", false),
    KEYCLOAK_TOKEN_FIRST_TIME_LOGIN(30006, "Please reset your password", false);



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