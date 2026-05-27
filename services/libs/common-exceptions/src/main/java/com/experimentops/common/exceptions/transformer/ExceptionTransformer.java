package com.experimentops.common.exceptions.transformer;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.dto.ErrorResponseDto;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public final class ExceptionTransformer {

    private ExceptionTransformer() {}


    public static ResponseEntity<ErrorResponseDto> transform(ExperimentOpsException exception) {
        String message = getRootCauseMessage(exception);
        HttpStatus status = exception.getErrorCode().isServerError()
                ? INTERNAL_SERVER_ERROR
                : BAD_REQUEST;
        return response(exception.getErrorCode().getCode(), message, status);
    }


    public static ResponseEntity<ErrorResponseDto> transform(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();

        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            return response(ErrorCode.INVALID_INPUTS.getCode(), fieldErrors.getFirst().getDefaultMessage(), BAD_REQUEST);
        }

        List<ObjectError> globalErrors = bindingResult.getGlobalErrors();
        if (!globalErrors.isEmpty()) {
            return response(ErrorCode.GLOBAL_ERROR.getCode(), globalErrors.getFirst().getDefaultMessage(), BAD_REQUEST);
        }

        return response(ErrorCode.GLOBAL_ERROR.getCode(), getRootCauseMessage(exception), BAD_REQUEST);
    }

    // ── Bad request variants ─────────────────────────────────────

    public static ResponseEntity<ErrorResponseDto> badRequest(Exception exception) {
        return response(ErrorCode.INVALID_INPUTS.getCode(), getRootCauseMessage(exception), BAD_REQUEST);
    }

    public static ResponseEntity<ErrorResponseDto> transformNotFound(Exception exception) {
        return response(ErrorCode.RESOURCE_NOT_FOUND.getCode(), getRootCauseMessage(exception), HttpStatus.NOT_FOUND);
    }

    public static ResponseEntity<ErrorResponseDto> transformUnauthorized(Exception exception) {
        return response(ErrorCode.AUTHORIZATION_ERROR.getCode(), getRootCauseMessage(exception), HttpStatus.UNAUTHORIZED);
    }

    // ── Generic / internal ───────────────────────────────────────

    public static ResponseEntity<ErrorResponseDto> internalError(Exception exception) {
        return response(ErrorCode.GENERAL_ERROR.getCode(), getRootCauseMessage(exception), INTERNAL_SERVER_ERROR);
    }


    private static ResponseEntity<ErrorResponseDto> response(int errorCode, String message, HttpStatus status) {
        return new ResponseEntity<>(new ErrorResponseDto(errorCode, message), status);
    }

    private static String getRootCauseMessage(Exception exception) {
        String message = null;

        if (exception instanceof NestedRuntimeException nestedException) {
            Throwable rootCause = nestedException.getRootCause();
            if (rootCause != null) {
                message = rootCause.getMessage();
            }
        }

        if (message == null) {
            message = exception.getMessage();
        }

        return message;
    }
}