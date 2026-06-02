package com.experimentops.platformapi.workspace;

import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.platformapi.BaseMysqlTest;
import com.experimentops.platformapi.PlatformApiApplication;
import com.experimentops.platformapi.dal.gateway.KeyCloakGateway;
import com.experimentops.workspace.model.v1.WorkspaceAdminUserRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceRequestModel;
import com.experimentops.workspace.model.v1.WorkspaceResponseModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "10000")
@ActiveProfiles("test")
@Sql({"/schema.sql", "/data.sql"})
@DirtiesContext
class WorkspaceControllerTest extends BaseMysqlTest {

    private static final String WORKSPACE_API_PATH = "/v1/workspaces";
    private static final String X_TOKEN_C_USER_ROLE = "X-Token-C-User-Role";
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String WORKSPACE_ADMIN = "WORKSPACE_ADMIN";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @MockitoBean
    private KeyCloakGateway keyCloakGateway;

    @Test
    void createWorkspace_withValidRequest_returns202AndBody() {
        WorkspaceRequestModel request = buildRequest(
                "New Test Workspace",
                "admin@newtestworkspace.com",
                "jane.doe@newtestworkspace.com"
        );

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, PLATFORM_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(WorkspaceResponseModel.class)
                .value(response -> {
                    assertThat(response.getWorkspaceName()).isEqualTo("New Test Workspace");
                    assertThat(response.getWorkspaceEmail()).isEqualTo("admin@newtestworkspace.com");
                    assertThat(response.getWorkspaceUuid()).isNotBlank();
                });
    }

    @Test
    void createWorkspace_withDuplicateName_returns400() {
        // "Existing Workspace" is seeded by data.sql; duplicate check fires on name match
        WorkspaceRequestModel request = buildRequest(
                "Existing Workspace",
                "new@unique.com",
                "admin@unique.com"
        );

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, PLATFORM_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createWorkspace_withDuplicateEmail_returns400() {
        // "existing@workspace.com" is seeded by data.sql; duplicate check fires on email+ACTIVE+enabled
        WorkspaceRequestModel request = buildRequest(
                "Brand New Workspace",
                "existing@workspace.com",
                "admin@brandnew.com"
        );

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, PLATFORM_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createWorkspace_withMissingWorkspaceName_returns400() {
        WorkspaceRequestModel request = new WorkspaceRequestModel()
                .workspaceEmail("admin@workspace.com")
                .adminUser(new WorkspaceAdminUserRequestModel()
                        .firstName("Jane")
                        .lastName("Doe")
                        .email("jane.doe@workspace.com")
                        .password("Test@12345"));

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, PLATFORM_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createWorkspace_withMissingAdminUser_returns400() {
        WorkspaceRequestModel request = new WorkspaceRequestModel()
                .workspaceName("Valid Workspace")
                .workspaceEmail("admin@validworkspace.com");

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, PLATFORM_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createWorkspace_withNoAuthHeader_returns401() {
        WorkspaceRequestModel request = buildRequest(
                "Another Workspace",
                "admin@another.com",
                "jane@another.com"
        );

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createWorkspace_withInsufficientRole_returns401() {
        // WORKSPACE_ADMIN lacks ADD_WORKSPACE permission; AccessDeniedException -> GlobalExceptionHandler -> 401
        WorkspaceRequestModel request = buildRequest(
                "Yet Another Workspace",
                "admin@yetanother.com",
                "jane@yetanother.com"
        );

        webTestClient.post()
                .uri(WORKSPACE_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(X_TOKEN_C_USER_ROLE, WORKSPACE_ADMIN)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WorkspaceRequestModel buildRequest(String workspaceName, String workspaceEmail, String adminEmail) {
        return new WorkspaceRequestModel()
                .workspaceName(workspaceName)
                .workspaceEmail(workspaceEmail)
                .adminUser(new WorkspaceAdminUserRequestModel()
                        .firstName("Test")
                        .lastName("Admin")
                        .email(adminEmail)
                        .password("Test@12345"));
    }
}
