package com.experimentops.service.scan;

public class DatasetScanException extends RuntimeException {
    public DatasetScanException(String message) {
        super(message);
    }

    public DatasetScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
