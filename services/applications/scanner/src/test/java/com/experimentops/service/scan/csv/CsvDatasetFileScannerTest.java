package com.experimentops.service.scan.csv;

import com.experimentops.service.scan.DatasetFileScanRequest;
import com.experimentops.service.scan.DatasetScanException;
import com.experimentops.utils.JSONUtil;
import com.experimentops.validator.CsvDatasetFileValidator;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvDatasetFileScannerTest {
    private final CsvDatasetFileScanner scanner = new CsvDatasetFileScanner(new CsvDatasetFileValidator());

    @Test
    void scan_extractsColumnsAndFirstFiftyRows() throws Exception {
        StringBuilder csv = new StringBuilder("id,name\n");
        for (int index = 1; index <= 55; index++) {
            csv.append(index).append(",name-").append(index).append('\n');
        }
        DatasetFileScanRequest request = new DatasetFileScanRequest(
                "dataset.csv",
                "CSV",
                "s3://bucket/dataset.csv",
                (long) csv.length(),
                csv.toString().getBytes(StandardCharsets.UTF_8)
        );

        JsonNode preview = JSONUtil.toObjectFromTypedJson(
                new String(scanner.scan(request).previewJson(), StandardCharsets.UTF_8),
                JsonNode.class
        );

        assertThat(preview.get("format").asText()).isEqualTo("CSV");
        assertThat(preview.get("columns")).hasSize(2);
        assertThat(preview.get("columns").get(0).asText()).isEqualTo("id");
        assertThat(preview.get("columns").get(1).asText()).isEqualTo("name");
        assertThat(preview.get("rows")).hasSize(50);
        assertThat(preview.get("rows").get(0).get("id").asText()).isEqualTo("1");
        assertThat(preview.get("rows").get(49).get("name").asText()).isEqualTo("name-50");
        assertThat(preview.get("sampleRowCount").asInt()).isEqualTo(50);
    }

    @Test
    void scan_whenHeaderContainsBlankColumn_throwsScanException() {
        DatasetFileScanRequest request = new DatasetFileScanRequest(
                "dataset.csv",
                "CSV",
                "s3://bucket/dataset.csv",
                13L,
                "id,\n1,a\n".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> scanner.scan(request))
                .isInstanceOf(DatasetScanException.class)
                .hasMessageContaining("Unable to parse CSV dataset file");
    }
}
