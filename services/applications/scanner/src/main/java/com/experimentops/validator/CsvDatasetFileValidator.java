package com.experimentops.validator;

import com.experimentops.service.scan.DatasetScanException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CsvDatasetFileValidator {

    public void validateColumns(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new DatasetScanException("CSV dataset file is missing a header row");
        }
        boolean hasBlankColumn = columns.stream().anyMatch(StringUtils::isBlank);
        if (hasBlankColumn) {
            throw new DatasetScanException("CSV dataset file contains a blank header column");
        }
    }
}
