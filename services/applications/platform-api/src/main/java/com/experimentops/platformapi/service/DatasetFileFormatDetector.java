package com.experimentops.platformapi.service;

import com.experimentops.platformapi.model.type.DatasetFileFormatEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DatasetFileFormatDetector {

    public DatasetFileFormatEnum detect(String filename, String contentType) {
        String lowerFilename = StringUtils.defaultString(filename).toLowerCase();
        String lowerContentType = StringUtils.defaultString(contentType).toLowerCase();

        if (lowerFilename.endsWith(".csv") || "text/csv".equals(lowerContentType) || "application/csv".equals(lowerContentType)) {
            return DatasetFileFormatEnum.CSV;
        }

        if (lowerFilename.endsWith(".xlsx")
                || lowerFilename.endsWith(".xls")
                || "application/vnd.ms-excel".equals(lowerContentType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(lowerContentType)) {
            return DatasetFileFormatEnum.EXCEL;
        }

        if (lowerFilename.endsWith(".txt") || "text/plain".equals(lowerContentType)) {
            return DatasetFileFormatEnum.TEXT;
        }

        return DatasetFileFormatEnum.UNKNOWN;
    }
}
