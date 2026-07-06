package com.experimentops.service.scan;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatasetFileScannerRegistry {
    private final List<DatasetFileScanner> scanners;

    public Optional<DatasetFileScanner> findScanner(DatasetFileScanRequest request) {
        return scanners.stream()
                .filter(scanner -> scanner.supports(request))
                .findFirst();
    }
}
