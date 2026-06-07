package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.UserService;
import com.experimentops.user.api.v1.UserApi;
import com.experimentops.user.model.v1.UserRequestModel;
import com.experimentops.user.model.v1.UserResponseModel;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class UserController implements UserApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(UserController.class);

    private final HttpServletRequest exchange;
    private final UserService userService;

    @PreAuthorize("hasAuthority('" + PermissionConstants.ADD_USER + "')")
    @Override
    public ResponseEntity<UserResponseModel> addUser(UserRequestModel userRequestModel) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        log.info(experimentOpsHeaders, "Adding user : " + userRequestModel);
        UserResponseModel userModel = userService.publishUser(userRequestModel, experimentOpsHeaders);
        return ResponseEntity.ok(userModel);
    }
}
