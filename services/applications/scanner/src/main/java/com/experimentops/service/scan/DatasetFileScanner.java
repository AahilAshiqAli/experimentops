package com.experimentops.service.scan;


public interface DatasetFileScanner {
    boolean supports(DatasetFileScanRequest request);

    DatasetFileScanResult scan(DatasetFileScanRequest request);
}
