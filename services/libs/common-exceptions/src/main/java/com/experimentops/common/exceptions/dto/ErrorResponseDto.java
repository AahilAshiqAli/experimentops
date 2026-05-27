package com.experimentops.common.exceptions.dto;

// Record already does all the work lombok does
public record ErrorResponseDto(int errorCode, String message) {}
