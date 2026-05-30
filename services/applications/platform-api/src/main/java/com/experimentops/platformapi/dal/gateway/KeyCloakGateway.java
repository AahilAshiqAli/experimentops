package com.experimentops.platformapi.dal.gateway;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.platformapi.common.exceptions.KeycloakException;
import com.experimentops.platformapi.transformer.KeyCloakTransformer;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import com.experimentops.workspace.event.WorkspaceMutationEventPayload;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
public class KeyCloakGateway {

    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(KeyCloakGateway.class);

    private static final String WORKSPACE_ADMIN_ROLE = "WORKSPACE_ADMIN";
    private static final String RESEARCHER_ROLE = "RESEARCHER";

    private final Keycloak keycloak;
    private final KeyCloakTransformer keycloakTransformer;

    @Value("${experimentops.keycloak.auth.token-url}")
    private String tokenUrl;
  // Should not use KeyCloak SDK here as only for user authentication which is a small job, should not instantiate a new heavy object KeyCloakBuilder
    public AccessTokenResponse authenticate(String realmName, AuthLoginRequest authLoginRequest, ExperimentOpsHeaders experimentOpsHeaders) {
        log.info(experimentOpsHeaders, "Authenticating keycloak realm: " + realmName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", authLoginRequest.getUsername());
        body.add("password", authLoginRequest.getPassword());
        body.add("client_id", authLoginRequest.getClientId());
        body.add("grant_type", StringUtils.hasText(authLoginRequest.getGrantType()) ? authLoginRequest.getGrantType() : OAuth2Constants.PASSWORD);


        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return new RestTemplate().postForObject(resolveTokenUrl(realmName), request, AccessTokenResponse.class);
    }

    public void createWorkspaceRealm(WorkspaceMutationEvent workspaceMutationEvent, ExperimentOpsHeaders experimentopsHeaders) {
        WorkspaceMutationEventPayload workspaceEvent = workspaceMutationEvent.getPayload();
        String realmName = workspaceEvent.getWorkspaceName();

        log.info(experimentopsHeaders, "Creating workspace realm: " + realmName);

        keycloak.realms().create(
                keycloakTransformer.transformRealm(realmName, workspaceMutationEvent)
        );

        log.info(experimentopsHeaders, "Created workspace realm: " + realmName);

        keycloak.realm(realmName)
                .components()
                .add(keycloakTransformer.mapComponentExportRepresentation())
                .close();

        createRealmRole(realmName, WORKSPACE_ADMIN_ROLE);
        createRealmRole(realmName, RESEARCHER_ROLE);

        createWorkspaceAdminUser(workspaceEvent, experimentopsHeaders, workspaceMutationEvent.getMetadata().getUuid());
    }

    public void createWorkspaceAdminUser(WorkspaceMutationEventPayload workspaceEvent, ExperimentOpsHeaders experimentopsHeaders, String workspaceUuid) {
        String realmName = workspaceEvent.getWorkspaceName();

        log.info(experimentopsHeaders, "Creating workspace admin user: " + workspaceEvent.getAdminFirstName());

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(workspaceEvent.getAdminEmail());
        userRepresentation.setFirstName(workspaceEvent.getAdminFirstName());
        userRepresentation.setLastName(workspaceEvent.getAdminLastName());
        userRepresentation.setEmail(workspaceEvent.getAdminEmail());
        userRepresentation.setCredentials(createPasswordCredential(workspaceEvent));
        userRepresentation.setRealmRoles(Collections.singletonList(WORKSPACE_ADMIN_ROLE));
        userRepresentation.setAttributes(keycloakTransformer.createUserAttributes(workspaceUuid, workspaceEvent.getUserUuid())
        );

        Response response = keycloak.realm(realmName)
                .users()
                .create(userRepresentation);

        if (response.getStatus() != HttpStatus.CREATED.value()) {
            throw new ValidationException(
                    ErrorCode.EMAIL_OR_USERNAME_ALREADY_EXISTS,
                    String.format("User already exists with email: %s", workspaceEvent.getAdminEmail())
            );
        }

        String userId = CreatedResponseUtil.getCreatedId(response);
        assignRealmRoleToUser(realmName, userId, WORKSPACE_ADMIN_ROLE);

        log.info(experimentopsHeaders, "Created workspace admin user: " + workspaceEvent.getAdminEmail());
    }

    public void createRealmRole(String realmName, String roleName) {
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName(roleName);
        roleRepresentation.setClientRole(false);

        try {
            keycloak.realm(realmName)
                    .roles()
                    .create(roleRepresentation);
        } catch (Exception ex) {
            throw new KeycloakException(ErrorCode.ROLE_CREATION_FAILED, ex);
        }
    }

    public RoleRepresentation getRoleRepresentation(String realmName, String roleName) {
        return keycloak.realm(realmName)
                .roles()
                .get(roleName)
                .toRepresentation();
    }

    private void assignRealmRoleToUser(String realmName, String userId, String roleName) {
        RoleRepresentation roleRepresentation = getRoleRepresentation(realmName, roleName);

        UserResource userResource = keycloak.realm(realmName)
                .users()
                .get(userId);

        userResource.roles()
                .realmLevel()
                .add(Collections.singletonList(roleRepresentation));
    }

    private List<CredentialRepresentation> createPasswordCredential(WorkspaceMutationEventPayload workspaceEvent) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(true);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(workspaceEvent.getAdminPassword());

        List<CredentialRepresentation> credentials = new ArrayList<>();
        credentials.add(credentialRepresentation);

        return credentials;
    }

    private String resolveTokenUrl(String realmName) {
        return tokenUrl.replace("{realmName}", realmName);
    }
}
