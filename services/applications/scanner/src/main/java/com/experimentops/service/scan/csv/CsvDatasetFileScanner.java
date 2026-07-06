package com.experimentops.service.scan.csv;

import com.experimentops.service.scan.DatasetFileScanRequest;
import com.experimentops.service.scan.DatasetFileScanResult;
import com.experimentops.service.scan.DatasetFileScanner;
import com.experimentops.service.scan.DatasetScanException;
import com.experimentops.utils.JSONUtil;
import com.experimentops.validator.CsvDatasetFileValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class CsvDatasetFileScanner implements DatasetFileScanner {
    private static final int PREVIEW_ROW_LIMIT = 50;

    private final CsvDatasetFileValidator validator;

    @Override
    public boolean supports(DatasetFileScanRequest request) {
        if (request == null) {
            return false;
        }
        String format = normalize(request.format());
        String originalName = normalize(request.originalName());
        return "CSV".equals(format)
                || "TEXT/CSV".equals(format)
                || "APPLICATION/CSV".equals(format)
                || originalName.endsWith(".CSV")
                || looksLikeCsv(request.content());
    }

    @Override
    public DatasetFileScanResult scan(DatasetFileScanRequest request) {
        CsvPreview preview = parsePreview(request);
        String previewJson = JSONUtil.toNonTypedJsonFromObject(preview);
        if (StringUtils.isBlank(previewJson)) {
            throw new DatasetScanException("Unable to serialize CSV preview");
        }
        return new DatasetFileScanResult(previewJson.getBytes(StandardCharsets.UTF_8));
    }

    private CsvPreview parsePreview(DatasetFileScanRequest request) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(request.content()), StandardCharsets.UTF_8);
             CSVParser parser = csvFormat().parse(reader)) {
            List<String> columns = parser.getHeaderMap()
                    .keySet()
                    .stream()
                    .map(this::removeUtf8Bom)
                    .toList();
            validator.validateColumns(columns);

            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                if (rows.size() == PREVIEW_ROW_LIMIT) {
                    break;
                }
                rows.add(toPreviewRow(columns, csvRecord));
            }

            return new CsvPreview("CSV", columns, rows, rows.size());
        } catch (IOException | IllegalArgumentException exception) {
            throw new DatasetScanException("Unable to parse CSV dataset file", exception);
        }
    }

    private CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
    }

    private Map<String, String> toPreviewRow(List<String> columns, CSVRecord csvRecord) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            String value = columnIndex < csvRecord.size() ? csvRecord.get(columnIndex) : "";
            row.put(columns.get(columnIndex), value);
        }
        return row;
    }

    private boolean looksLikeCsv(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        String sample = new String(content, 0, Math.min(content.length, 4096), StandardCharsets.UTF_8);
        return sample.lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(line -> line.contains(","))
                .orElse(false);
    }

    private String normalize(String value) {
        return StringUtils.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String removeUtf8Bom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private record CsvPreview(
            String format,
            List<String> columns,
            List<Map<String, String>> rows,
            int sampleRowCount
    ) {
    }
}
