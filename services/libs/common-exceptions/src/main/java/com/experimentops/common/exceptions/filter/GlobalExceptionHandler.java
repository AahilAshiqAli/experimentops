package com.experimentops.common.exceptions.filter;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.dto.ErrorResponseDto;
import com.experimentops.common.exceptions.transformer.ExceptionTransformer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_UUID = "requestUuid";
    private static final String USER_UUID = "X-Token-C-User-Uuid";
    private static final String WORKSPACE_UUID = "X-Token-C-Workspace-Uuid";
    private static final String TRACE_UUID = "X-Trace-Uuid";
    private static final String NA = "N/A";

    private final HttpServletRequest exchange;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> exception(AccessDeniedException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformUnauthorized(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> exception(HttpRequestMethodNotSupportedException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformNotFound(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> exception(NoResourceFoundException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformNotFound(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> exception(HttpMessageNotReadableException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(UnknownContentTypeException.class)
    public ResponseEntity<ErrorResponseDto> exception(UnknownContentTypeException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleException(MissingServletRequestParameterException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleException(MethodArgumentTypeMismatchException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transform(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(ExperimentOpsException.class)
    public ResponseEntity<ErrorResponseDto> exception(ExperimentOpsException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transform(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> exception(Exception exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.internalError(exception);
        logException(exception, response);
        return response;
    }

    protected void logException(@NonNull Exception exception,
                                @NonNull ResponseEntity<ErrorResponseDto> response) {
        ErrorResponseDto body = response.getBody();
        String statusCode = response.getStatusCode().toString();
        String cause = " | " + statusCode + (body == null ? "" : " | " + body);
        String message = formatHeaders()
                + " | "
                + exception.getClass().getName()
                + " | "
                + cause;

        log.error(message, exception);
    }

    private String formatHeaders() {
        String requestUuid = getHeaderOrDefault(REQUEST_UUID);
        String userUuid = getHeaderOrDefault(USER_UUID);
        String workspaceUuid = getHeaderOrDefault(WORKSPACE_UUID);
        String traceUuid = exchange.getHeader(TRACE_UUID);

        StringBuilder headers = new StringBuilder();
        headers.append("| requestUuid : ").append(requestUuid);
        headers.append(" | userUuid : ").append(userUuid);
        headers.append(" | workspaceUuid : ").append(workspaceUuid);
        if (traceUuid != null && !traceUuid.isBlank()) {
            headers.append(" | internalTraceUuid : ").append(traceUuid);
        }
        return headers.toString();
    }

    private String getHeaderOrDefault(String headerName) {
        String value = exchange.getHeader(headerName);
        return value == null || value.isBlank() ? NA : value;
    }
}
