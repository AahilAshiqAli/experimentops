package com.experimentops.common.exceptions.filter;

import com.experimentops.common.exceptions.ExperimentOpsException;
import com.experimentops.common.exceptions.dto.ErrorResponseDto;
import com.experimentops.common.exceptions.transformer.ExceptionTransformer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExperimentOpsException.class)
    public ResponseEntity<ErrorResponseDto> handleExperimentOpsException(
            ExperimentOpsException exception,
            HttpServletRequest request
    ) {
        log.warn("Handled application exception at {}: {}", request.getRequestURI(), exception.getMessage());
        return ExceptionTransformer.transform(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        log.warn("Validation failed at {}: {}", request.getRequestURI(), exception.getMessage());
        return ExceptionTransformer.transform(exception);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), exception.getMessage());
        return ExceptionTransformer.badRequest(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}", request.getRequestURI(), exception);
        return ExceptionTransformer.internalError(exception);
    }
}
