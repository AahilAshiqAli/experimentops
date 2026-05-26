package com.experimentops.common.exceptions.transformer;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

public final class ExceptionTransformer {
    private ExceptionTransformer() {
    }

    public static ResponseEntity<ErrorResponseDto> transform(ExperimentOpsException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        HttpStatus status = errorCode.isServerError()
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.BAD_REQUEST;

        return response(errorCode.getCode(), exception.getMessage(), status);
    }

    public static ResponseEntity<ErrorResponseDto> transform(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUTS.getMessage());

        return response(ErrorCode.INVALID_INPUTS.getCode(), message, HttpStatus.BAD_REQUEST);
    }
    public static ResponseEntity<ErrorResponseDto> badRequest(Exception exception) {
        return response(
                ErrorCode.INVALID_INPUTS.getCode(),
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    public static ResponseEntity<ErrorResponseDto> internalError(Exception exception) {
        return response(
                ErrorCode.GLOBAL_ERROR.getCode(),
                exception.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private static ResponseEntity<ErrorResponseDto> response(
            int errorCode,
            String message,
            HttpStatus status
    ) {
        return new ResponseEntity<>(new ErrorResponseDto(errorCode, message), status);
    }
}
