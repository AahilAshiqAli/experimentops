package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.AuthService;
import com.experimentops.user.api.v1.AuthApi;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.user.model.v1.AuthLoginResponse;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/")
public class AuthController implements AuthApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(AuthController.class);

    private final HttpServletRequest exchange;
    private final AuthService authService;

    @Override
    public ResponseEntity<AuthLoginResponse> loginRealm(String workspaceName, AuthLoginRequest authLoginRequest) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        log.info(experimentOpsHeaders, "Authenticating realm : " + workspaceName);
        return ResponseEntity.ok(authService.authenticate(workspaceName, authLoginRequest, experimentOpsHeaders));
    }
}
