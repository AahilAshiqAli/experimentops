package com.experimentops.common.exceptions.filter;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.dto.ErrorResponseDto;
import com.experimentops.common.exceptions.transformer.ExceptionTransformer;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.HeaderUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(GlobalExceptionHandler.class);

    private final HttpServletRequest exchange;

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(AccessDeniedException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformUnauthorized(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(HttpRequestMethodNotSupportedException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformNotFound(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(NoResourceFoundException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transformNotFound(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(HttpMessageNotReadableException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(UnknownContentTypeException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(UnknownContentTypeException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleException(MissingServletRequestParameterException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleException(MethodArgumentTypeMismatchException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.badRequest(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transform(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(ExperimentOpsException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(ExperimentOpsException exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.transform(exception);
        logException(exception, response);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDto> exception(Exception exception) {
        ResponseEntity<ErrorResponseDto> response = ExceptionTransformer.internalError(exception);
        logException(exception, response);
        return response;
    }

    protected void logException(@NonNull Exception exception,
                                @NonNull ResponseEntity<? extends ErrorResponseDto> response) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);

        ErrorResponseDto body = response.getBody();
        String statusCode = response.getStatusCode().toString();
        String cause = " | " + statusCode + (body == null ? "" : " | " + body);
        String message = exception.getClass().getName() + " | " + cause;

        log.error(headers, message, exception);
    }
}