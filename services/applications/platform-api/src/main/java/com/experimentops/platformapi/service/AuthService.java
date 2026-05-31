package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.exceptions.KeycloakException;
import com.experimentops.platformapi.dal.gateway.KeyCloakGateway;
import com.experimentops.platformapi.dal.repository.UserResetPasswordRepository;
import com.experimentops.platformapi.dal.repository.UserRepository;
import com.experimentops.platformapi.dal.repository.WorkspaceRepository;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.platformapi.model.entity.UserResetPassword;
import com.experimentops.platformapi.model.entity.Workspace;
import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.platformapi.validator.AuthValidator;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.user.model.v1.AuthLoginResponse;
import com.experimentops.user.model.v1.AuthResetPasswordRequest;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.ExperimentOpsUtils;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(AuthService.class);

    private final KeyCloakGateway keyCloakGateway;
    private final AuthValidator authValidator;
    private final UserResetPasswordRepository userResetPasswordRepository;
    private final UserTransformer userTransformer;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Value("${user.forgot.password.expiry}")
    long userForgotPasswordExpiry;

    @NonNull
    public AuthLoginResponse authenticate(@NonNull String realmName,
                                          AuthLoginRequest authLoginRequest,
                                          @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "authenticating realm : " + realmName);
        authValidator.validateAuthLoginRequestModel(authLoginRequest);
        Workspace workspace = getWorkspace(realmName);
        User user = getUser(authLoginRequest.getUsername(), workspace.getUuid());

        try {
            AccessTokenResponse accessTokenResponse = keyCloakGateway.authenticate(realmName, authLoginRequest, headers);
            return new AuthLoginResponse()
                    .accessToken(accessTokenResponse.getToken())
                    .expiresIn(accessTokenResponse.getExpiresIn())
                    .refreshToken(accessTokenResponse.getRefreshToken())
                    .refreshExpiresIn(accessTokenResponse.getRefreshExpiresIn())
                    .tokenType(accessTokenResponse.getTokenType())
                    .sessionState(accessTokenResponse.getSessionState())
                    .scope(accessTokenResponse.getScope());
        }
        catch( KeycloakException exception){
            if (ErrorCode.KEYCLOAK_TOKEN_FIRST_TIME_LOGIN == exception.getErrorCode()){
                UserResetPassword userResetPassword = userTransformer.transformUserResetPassword(user, workspace.getName());
                String entityUuid = userResetPasswordRepository.save(userResetPassword).getUuid();
                throw new KeycloakException(exception.getErrorCode(), entityUuid);
            }
            throw exception;
        }
    }

    public void verifyAndResetPassword(AuthResetPasswordRequest resetPasswordRequest, ExperimentOpsHeaders headers) {
        UserResetPassword userResetPassword = userResetPasswordRepository.findByUuidAndEnabled(resetPasswordRequest.getToken(), true)
                .orElseThrow(() ->
                        new ValidationException(ErrorCode.TOKEN_NOT_FOUND, ErrorCode.TOKEN_NOT_FOUND.getMessage()));
        String realmName = userResetPassword.getWorkspaceName();
        long difference = ExperimentOpsUtils.getSecondsDifference(userResetPassword.getCreationDate());
        if(difference > userForgotPasswordExpiry) {
            throw new KeycloakException(ErrorCode.RESET_PASSWORD_TOKEN_EXPIRED);
        }
        keyCloakGateway.updateUserPassword(realmName, userResetPassword.getEmail(),
                resetPasswordRequest.getNewPassword(), headers);
    }

    private Workspace getWorkspace(String workspaceName){
        return workspaceRepository
                .findByNameAndStatusAndEnabled(workspaceName, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        ErrorCode.WORKSPACE_NOT_FOUND.getMessage()
                ));
    }

    private User getUser(String email, String workspaceUuid){
        return userRepository
                .findByEmailAndWorkspaceUuidAndStatusAndEnabled(email, workspaceUuid, StatusEnum.ACTIVE, true)
                .orElseThrow(() -> new ValidationException(
                        ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getMessage()
                ));
    }
}
