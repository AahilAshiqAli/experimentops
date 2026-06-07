package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.dataset.api.v1.DatasetApi;
import com.experimentops.dataset.model.v1.DatasetResponseModel;
import com.experimentops.platformapi.service.DatasetService;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.constant.PermissionConstants;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class DatasetController implements DatasetApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(DatasetController.class);

    private final HttpServletRequest exchange;
    private final DatasetService datasetService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_DATASET + "')")
    @Override
    public ResponseEntity<DatasetResponseModel> uploadDataset(String experimentUuid, MultipartFile file) {
        ExperimentOpsHeaders headers = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.ok(datasetService.uploadDataset(experimentUuid, file, headers));
    }


}
