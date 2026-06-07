package com.experimentops.platformapi.dal.gateway;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.common.exceptions.KeycloakException;
import com.experimentops.platformapi.transformer.KeyCloakTransformer;
import com.experimentops.platformapi.transformer.UserTransformer;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.user.event.UserMutationEventPayload;
import com.experimentops.user.model.v1.AuthLoginRequest;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.event.WorkspaceMutationEvent;
import com.experimentops.workspace.event.WorkspaceMutationEventPayload;
import lombok.RequiredArgsConstructor;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import java.util.*;

@RequiredArgsConstructor
@Component
public class KeyCloakGateway {

    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(KeyCloakGateway.class);

    private static final String WORKSPACE_ADMIN_ROLE = "WORKSPACE_ADMIN";
    private static final String RESEARCHER_ROLE = "RESEARCHER";

    private final Keycloak keycloak;
    private final KeyCloakTransformer keycloakTransformer;
    private final RestTemplate restTemplate;
    private final UserTransformer userTransformer;

    @Value("${experimentops.keycloak.auth.token-url}")
    private String tokenUrl;
  // Should not use KeyCloak SDK here as only for user authentication which is a small job, should not instantiate a new heavy object KeyCloakBuilder
      public AccessTokenResponse authenticate(String realmName, AuthLoginRequest authLoginRequest, ExperimentOpsHeaders experimentOpsHeaders) {
          log.info(experimentOpsHeaders, "Authenticating keycloak realm: " + realmName);

          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

          MultiValueMap<String, String> body = keycloakTransformer.transformLoginRequestModel(authLoginRequest.getUsername(), authLoginRequest.getPassword());

          HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
          String url = resolveTokenUrl(realmName);
          log.info(experimentOpsHeaders, "making api call on " + url);
          try {
              return restTemplate.postForObject(url, request, AccessTokenResponse.class);
          } catch (HttpClientErrorException exception) {
              log.info(experimentOpsHeaders, "ResponseBody: " + exception.getResponseBodyAsString());
              log.info(experimentOpsHeaders, "Message: " + exception.getMessage());
              log.info(experimentOpsHeaders, "StatusText: " + exception.getStatusText());

              ErrorCode errorCode = ErrorCode.KEYCLOAK_TOKEN_ERROR;
              if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                  errorCode = ErrorCode.KEYCLOAK_TOKEN_INVALID_CREDENTIALS;
              }
              if (!StringUtils.isBlank(exception.getResponseBodyAsString())) {
                  Map<String, String> errorResponse = JSONUtil.toObjectFromTypedJson(
                          exception.getResponseBodyAsString(), Map.class);
                  if (ErrorCode.KEYCLOAK_ACCOUNT_NOT_FULLY_SETUP.getMessage()
                          .equalsIgnoreCase(errorResponse.get("error_description"))) {
                      errorCode = ErrorCode.KEYCLOAK_TOKEN_FIRST_TIME_LOGIN;
                  }
              }
              throw new KeycloakException(errorCode, exception);
          }
      }

    public void createWorkspaceRealm(WorkspaceMutationEvent workspaceMutationEvent, ExperimentOpsHeaders experimentopsHeaders) {
        WorkspaceMutationEventPayload workspaceEvent = workspaceMutationEvent.getPayload();
        String realmName = workspaceEvent.getWorkspaceName();

        log.info(experimentopsHeaders, "Creating workspace realm: " + realmName);

        keycloak.realms().create(
                keycloakTransformer.transformRealm(realmName, workspaceMutationEvent)
        );

        log.info(experimentopsHeaders, "Created workspace realm: " + realmName);

        // Adding private openssl key to generate realm rsa key
        try (Response componentResponse = keycloak.realm(realmName)
                .components()
                .add(keycloakTransformer.mapComponentExportRepresentation())) {
            if (componentResponse.getStatus() != HttpStatus.CREATED.value()) {
                throw new KeycloakException(ErrorCode.ROLE_CREATION_FAILED,
                        new RuntimeException("Failed to inject RSA key into realm: " + realmName
                                + " status=" + componentResponse.getStatus()
                                + " body=" + componentResponse.readEntity(String.class)));
            }
        }

        // Adding roles in realm
        createRealmRole(realmName, WORKSPACE_ADMIN_ROLE);
        createRealmRole(realmName, RESEARCHER_ROLE);

        // Add user attributes in realm settings user profile
        addUserAttributes(realmName);

        UserMutationEvent userMutationEvent = userTransformer.transformUserCreationEvent(workspaceMutationEvent, experimentopsHeaders);
        experimentopsHeaders.setWorkspaceUuid(workspaceMutationEvent.getMetadata().getUuid());
        createWorkspaceUser(userMutationEvent, experimentopsHeaders, WORKSPACE_ADMIN_ROLE, workspaceEvent.getWorkspaceName());
    }

    private void addUserAttributes(String realmName) {
        try (Response response = keycloak.realm(realmName)
                .users()
                .userProfile()
                .update(JSONUtil.toNonTypedJsonFromObject(keycloakTransformer.createUserProfilePayload()))) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new KeycloakException(ErrorCode.ROLE_CREATION_FAILED,
                        new RuntimeException("Failed to update user profile for realm: " + realmName
                                + " status=" + response.getStatus()
                                + " body=" + response.readEntity(String.class)));
            }
        }
    }

    public void createWorkspaceUser(UserMutationEvent userEvent, ExperimentOpsHeaders experimentopsHeaders, String role, String realmName) {
        UserMutationEventPayload userMutationEventPayload = userEvent.getPayload();
        String userUuid = userEvent.getMetadata().getUuid();

        log.info(experimentopsHeaders, "Creating workspace user: " + userMutationEventPayload.getUserFirstName() + " with role " + role);

        UserRepresentation userRepresentation = new UserRepresentation();
        Map<String, List<String>> userAttributes = keycloakTransformer.createUserAttributes(experimentopsHeaders.getWorkspaceUuid(), userUuid);
        userRepresentation.setId(userUuid);
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(userMutationEventPayload.getUserEmail());
        userRepresentation.setFirstName(userMutationEventPayload.getUserFirstName());
        userRepresentation.setLastName(userMutationEventPayload.getUserLastName());
        userRepresentation.setEmail(userMutationEventPayload.getUserEmail());
        userRepresentation.setCredentials(createPasswordCredential(userMutationEventPayload.getUserPassword()));
        userRepresentation.setAttributes(userAttributes);

        log.info(experimentopsHeaders, "Creating workspace user attributes: " + userAttributes);

        Response response = keycloak.realm(realmName)
                .users()
                .create(userRepresentation);

        if (response.getStatus() != HttpStatus.CREATED.value()) {
            throw new ValidationException(
                    ErrorCode.EMAIL_OR_USERNAME_ALREADY_EXISTS,
                    String.format("User already exists with email: %s", userMutationEventPayload.getUserEmail())
            );
        }

        String userId = CreatedResponseUtil.getCreatedId(response);
        assignRealmRoleToUser(realmName, userId, role);

        log.info(experimentopsHeaders, "Created workspace user: " + userMutationEventPayload.getUserEmail() + " with role " + role );
    }

    public void updateUserPassword(String realm, String userName, String newPassword, ExperimentOpsHeaders experimentOpsHeaders) {
        UserRepresentation userRepresentation = getUserRepresentation(userName, realm, experimentOpsHeaders);
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(newPassword);
        credentialRepresentation.setTemporary(false);
        try {
            keycloak.realm(realm).users().get(userRepresentation.getId()).resetPassword(credentialRepresentation);
        } catch (Exception ex) {
            log.error(experimentOpsHeaders, "Unable to update password: " + ex.getMessage(), ex);
            throw new KeycloakException(ErrorCode.KEYCLOAK_PASSWORD_VALIDATION);
        }
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

    private List<CredentialRepresentation> createPasswordCredential(String password) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(true);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(password);

        List<CredentialRepresentation> credentials = new ArrayList<>();
        credentials.add(credentialRepresentation);

        return credentials;
    }

    public UserRepresentation getUserRepresentation(String userName, String realm, ExperimentOpsHeaders experimentOpsHeaders) {
        log.info(experimentOpsHeaders, "Calling keycloak to fetch users");
        List<UserRepresentation> users = keycloak.realm(realm).users().list(0, Integer.MAX_VALUE);
        log.info(experimentOpsHeaders, "Users size: " + users.size());
        log.info(experimentOpsHeaders, "Filtering user from: " + realm + " as per username: " + userName);
        Optional<UserRepresentation> currentUser = users.stream()
                .filter(userRepresentation -> userRepresentation.getUsername().equalsIgnoreCase(userName))
                .findAny();
        if (currentUser.isEmpty()) {
            throw new KeycloakException(ErrorCode.KEYCLOAK_USER_NOT_FOUND);
        }
        return currentUser.get();
    }

    private String resolveTokenUrl(String realmName) {
        return tokenUrl.replace("{realmName}", realmName);
    }
}
