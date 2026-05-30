package com.experimentops.platformapi.service;

import com.experimentops.platformapi.dal.gateway.KeyCloakGateway;
import com.experimentops.platformapi.validator.AuthValidator;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.user.model.v1.AuthLoginResponse;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(AuthService.class);

    private final KeyCloakGateway keyCloakGateway;
    private final AuthValidator authValidator;

    @NonNull
    public AuthLoginResponse authenticate(@NonNull String realmName,
                                          AuthLoginRequest authLoginRequest,
                                          @NonNull ExperimentOpsHeaders headers) {
        log.info(headers, "authenticating realm : " + realmName);
        authValidator.validateAuthLoginRequestModel(authLoginRequest);

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
}
