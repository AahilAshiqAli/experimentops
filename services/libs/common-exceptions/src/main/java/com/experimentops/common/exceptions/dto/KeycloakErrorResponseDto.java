package com.experimentops.common.exceptions.dto;

public record KeycloakErrorResponseDto(String entityId, int errorCode, String message) {}
