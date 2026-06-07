package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.AuthService;
import com.experimentops.user.api.v1.AuthApi;
import com.experimentops.user.model.v1.AuthForgotPasswordRequest;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.user.model.v1.AuthLoginResponse;
import com.experimentops.user.model.v1.AuthResetPasswordRequest;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.HeaderUtil;
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
public class AuthController implements AuthApi {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(AuthController.class);

    private final HttpServletRequest exchange;
    private final AuthService authService;

    @PreAuthorize("permitAll()")
    @Override
    public ResponseEntity<AuthLoginResponse> loginRealm(String workspaceName, AuthLoginRequest authLoginRequest) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        log.info(experimentOpsHeaders, "Authenticating realm : " + workspaceName);
        return ResponseEntity.ok(authService.authenticate(workspaceName, authLoginRequest, experimentOpsHeaders));
    }

    @PreAuthorize("permitAll()")
    @Override
    public ResponseEntity<Void> forgotPassword(AuthForgotPasswordRequest authForgotPasswordRequest) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        log.info(experimentOpsHeaders, "generating forgot password token");
        authService.forgetPassword(authForgotPasswordRequest, experimentOpsHeaders);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("permitAll()")
    @Override
    public ResponseEntity<Void> resetPassword(AuthResetPasswordRequest authResetPasswordRequest) {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        log.info(experimentOpsHeaders, "resetting password");
        authService.verifyAndResetPassword(authResetPasswordRequest, experimentOpsHeaders);
        return ResponseEntity.ok().build();
    }
}
