package com.experimentops.platformapi.transformer;

import com.experimentops.workspace.event.WorkspaceMutationEvent;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@Component
public class KeyCloakTransformer {

    private static final Logger log = LoggerFactory.getLogger(KeyCloakTransformer.class);

    @Value("${workspace.config.server.rsa-key}")
    private String rsaKey;

    @Value("${kc.smtp.mail.host}")
    private String smtpHost;

    @Value("${kc.smtp.mail.port}")
    private String smtpPort;

    @Value("${kc.smtp.mail.from}")
    private String smtpFrom;

    @Value("${kc.smtp.mail.auth}")
    private String smtpAuth;

    @Value("${kc.smtp.mail.user}")
    private String smtpUser;

    @Value("${kc.smtp.mail.password}")
    private String smtpPassword;

    @Value("${kc.smtp.mail.ssl}")
    private String sslEnabled;

    @Value("${kc.smtp.mail.start.tls}")
    private String startTls;

    @Value("${keycloak.password.policy.length}")
    private int keyCloakPasswordPolicyLength;

    @Value("${keycloak.password.policy.specialChars}")
    private int keyCloakPasswordPolicySpecialChars;

    @Value("${keycloak.password.policy.digits}")
    private int keyCloakPasswordPolicyDigits;

    @Value("${keycloak.password.policy.expiryDays}")
    private int keyCloakPasswordPolicyExpiryDays;

    @Value("${keycloak.password.policy.history}")
    private int keyCloakPasswordPolicyHistory;

    @Value("${experimentops.portal.url}")
    private String experimentOpsPortalUrl;

    private static final String OPENID_CONNECT = "openid-connect";
    private static final String WORKSPACE_UUID_ATTRIBUTE = "workspace_uuid";
    private static final String USER_UUID_ATTRIBUTE = "user_uuid";

    private static final String OFFLINE_ACCESS = "offline_access";
    private static final List<String> DEFAULT_ROLES = List.of(OFFLINE_ACCESS, "uma_authorization");
    private static final String EXPERIMENTOPS_CLIENT_ID = "experimentops-web";

    private static final String WORKSPACE_MAPPER_NAME = "WorkspaceUuidMapper";
    private static final String WORKSPACE_MAPPER_ATTRIBUTE = "workspace_uuid";
    private static final String USER_MAPPER_NAME = "UserUuidMapper";
    private static final String USER_MAPPER_ATTRIBUTE = "user_uuid";

    private static final String ID_TOKEN_CLAIM = "id.token.claim";
    private static final String ACCESS_TOKEN_CLAIM = "access.token.claim";
    private static final String USERINFO_TOKEN_CLAIM = "userinfo.token.claim";
    private static final String MULTIVALUED = "multivalued";
    private static final String AGGREGATE_ATTRIBUTES = "aggregate.attrs";
    private static final String USER_ATTRIBUTE = "user.attribute";
    private static final String CLAIM_NAME = "claim.name";
    private static final String JSONTYPE_LABEL = "jsonType.label";
    private static final String STRING = "String";
    private static final String REALM_ROLES_MAPPER_NAME = "RolesMapper";
    private static final String REALM_ROLES_CLAIM_NAME = "roles";


    public RealmRepresentation transformRealm(String realmName, WorkspaceMutationEvent workspaceMutationEvent) {

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setEnabled(true);
        realmRepresentation.setRealm(realmName);
        realmRepresentation.setDisplayName(realmName);
        List<ClientRepresentation> clients = new ArrayList<>();
        clients.add(addExperimentOpsClient());
        realmRepresentation.setClients(clients);

        realmRepresentation.setDuplicateEmailsAllowed(false);
        realmRepresentation.setEditUsernameAllowed(true);
        realmRepresentation.setRememberMe(true);
        realmRepresentation.setResetPasswordAllowed(true);
        realmRepresentation.setRegistrationAllowed(false);

        realmRepresentation.setAttributes(
                createRealmAttributes(workspaceMutationEvent.getMetadata().getUuid())
        );

        setPasswordPolicy(realmRepresentation);

        realmRepresentation.setMaxDeltaTimeSeconds(82800);
        realmRepresentation.setFailureFactor(5);
        realmRepresentation.setMaxFailureWaitSeconds(86400);
        realmRepresentation.setBruteForceProtected(true);

        realmRepresentation.setAccessTokenLifespan(2592000);
        realmRepresentation.setAccessCodeLifespanUserAction(7200);

        realmRepresentation.setDefaultRoles(DEFAULT_ROLES);
        realmRepresentation.setSmtpServer(setSmtpSetting());

        return realmRepresentation;
    }

    public ComponentRepresentation mapComponentExportRepresentation() {
        if (StringUtils.isBlank(rsaKey)) {
            log.warn("WORKSPACE_CONFIG_SERVER_RSA_KEY is not set — Keycloak will auto-generate realm keys. " +
                    "Tokens from this realm will have a unique kid and cannot be verified by the shared JWKS. " +
                    "Set WORKSPACE_CONFIG_SERVER_RSA_KEY to a valid PKCS8 private key.");
        }

        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setName("rsa");
        componentRepresentation.setProviderId("rsa");
        componentRepresentation.setProviderType("org.keycloak.keys.KeyProvider");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.put("priority", Collections.singletonList("101"));
        config.put("enabled", Collections.singletonList("true"));
        config.put("active", Collections.singletonList("true"));
        config.put("algorithm", Collections.singletonList("RS256"));
        config.put("privateKey", Collections.singletonList(normalizePemKey(rsaKey)));

        componentRepresentation.setConfig(config);

        return componentRepresentation;
    }

    private String normalizePemKey(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }
        String normalized = key.replace("\\n", "\n").trim();
        if (normalized.startsWith("-----")) {
            return normalized;
        }
        normalized = normalized.replaceAll("[^A-Za-z0-9+/=]", "");
        return "-----BEGIN PRIVATE KEY-----\n" + normalized + "\n-----END PRIVATE KEY-----";
    }

    public Map<String, List<String>> createUserAttributes(String workspaceUuid, String userUuid) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(WORKSPACE_UUID_ATTRIBUTE, Collections.singletonList(workspaceUuid));
        attributes.put(USER_UUID_ATTRIBUTE, Collections.singletonList(userUuid));
        return attributes;
    }

    private Map<String, String> createRealmAttributes(String workspaceUuid) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(WORKSPACE_UUID_ATTRIBUTE, workspaceUuid);
        return attributes;
    }

    private Map<String, String> setSmtpSetting() {
        Map<String, String> smtpServer = new HashMap<>();
        smtpServer.put("host", smtpHost);
        smtpServer.put("port", smtpPort);
        smtpServer.put("from", smtpFrom);
        smtpServer.put("auth", smtpAuth);
        smtpServer.put("user", smtpUser);
        smtpServer.put("password", smtpPassword);
        smtpServer.put("ssl", sslEnabled);
        smtpServer.put("starttls", startTls);
        return smtpServer;
    }

    private void setPasswordPolicy(RealmRepresentation realmRepresentation) {
        final String and = " and ";

        realmRepresentation.setPasswordPolicy(
                "length(" + keyCloakPasswordPolicyLength + ")" +
                        and +
                        "specialChars(" + keyCloakPasswordPolicySpecialChars + ")" +
                        and +
                        "digits(" + keyCloakPasswordPolicyDigits + ")" +
                        and +
                        "forceExpiredPasswordChange(" + keyCloakPasswordPolicyExpiryDays + ")" +
                        and +
                        "notUsername(undefined)" +
                        and +
                        "passwordHistory(" + keyCloakPasswordPolicyHistory + ")" +
                        and +
                        "hashAlgorithm(pbkdf2-sha256)"
        );
    }

    // Adding the client. KeyCloak already adds a client scope named experimentops-web-dedicated which would have mappers added by setProtocolMapping
    public ClientRepresentation addExperimentOpsClient() {
        ClientRepresentation clientRepresentation = new ClientRepresentation();

        clientRepresentation.setName("ExperimentOps Web");
        clientRepresentation.setClientId(EXPERIMENTOPS_CLIENT_ID);
        clientRepresentation.setEnabled(true);
        clientRepresentation.setProtocol(OPENID_CONNECT);


        clientRepresentation.setPublicClient(true);
        clientRepresentation.setStandardFlowEnabled(true);
        clientRepresentation.setDirectAccessGrantsEnabled(true);
        clientRepresentation.setImplicitFlowEnabled(false);

        clientRepresentation.setRedirectUris(List.of(
                experimentOpsPortalUrl + "/*"
        ));

        clientRepresentation.setWebOrigins(List.of(
                experimentOpsPortalUrl
        ));

        clientRepresentation.setDefaultClientScopes(List.of(
                "profile",
                "email",
                REALM_ROLES_CLAIM_NAME
        ));

        setProtocolMappers(clientRepresentation);

        return clientRepresentation;
    }

    public MultiValueMap<String, String> transformLoginRequestModel(String username, String password){
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        body.add("client_id", EXPERIMENTOPS_CLIENT_ID);
        body.add("grant_type", OAuth2Constants.PASSWORD);
        return body;
    }

    private void setProtocolMappers(ClientRepresentation clientRepresentation) {
        List<ProtocolMapperRepresentation> protocolMappers = new ArrayList<>();
        ProtocolMapperRepresentation realmRolesMapper = getProtocolMapperRepresentation();
        protocolMappers.add(realmRolesMapper);
        protocolMappers.addAll(mapProtocolMapperRepresentations());

        clientRepresentation.setProtocolMappers(protocolMappers);
    }

    public List<ProtocolMapperRepresentation> mapProtocolMapperRepresentations() {
        List<ProtocolMapperRepresentation> protocolMappers = new ArrayList<>();
        protocolMappers.add(addMappings(WORKSPACE_MAPPER_NAME, WORKSPACE_MAPPER_ATTRIBUTE));
        protocolMappers.add(addMappings(USER_MAPPER_NAME, USER_MAPPER_ATTRIBUTE));
        return protocolMappers;
    }

    private ProtocolMapperRepresentation addMappings(String mapperName, String mapperAttribute) {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName(mapperName);
        protocolMapperRepresentation.setProtocol(OPENID_CONNECT);
        protocolMapperRepresentation.setProtocolMapper("oidc-usermodel-attribute-mapper");

        Map<String, String> config = new HashMap<>();
        config.put(ID_TOKEN_CLAIM, "true");
        config.put(ACCESS_TOKEN_CLAIM, "true");
        config.put(USERINFO_TOKEN_CLAIM, "true");
        config.put(MULTIVALUED, "false");
        config.put(AGGREGATE_ATTRIBUTES, "false");
        config.put(USER_ATTRIBUTE, mapperAttribute);
        config.put(CLAIM_NAME, mapperAttribute);
        config.put(JSONTYPE_LABEL, STRING);
        protocolMapperRepresentation.setConfig(config);

        return protocolMapperRepresentation;
    }

    private static ProtocolMapperRepresentation getProtocolMapperRepresentation() {
        ProtocolMapperRepresentation realmRolesMapper = new ProtocolMapperRepresentation();
        realmRolesMapper.setProtocol(OPENID_CONNECT);
        realmRolesMapper.setName(REALM_ROLES_MAPPER_NAME);
        realmRolesMapper.setProtocolMapper("oidc-usermodel-realm-role-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(ID_TOKEN_CLAIM, "true");
        config.put(ACCESS_TOKEN_CLAIM, "true");
        config.put(USERINFO_TOKEN_CLAIM, "true");
        config.put(MULTIVALUED, "true");
        config.put(CLAIM_NAME, REALM_ROLES_CLAIM_NAME);
        config.put(JSONTYPE_LABEL, STRING);
        realmRolesMapper.setConfig(config);
        return realmRolesMapper;
    }

    public Map<String, Object> createUserProfilePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attributes", List.of(
                userProfileAttribute(
                        "username",
                        "${username}",
                        permissions(List.of("admin", "user"), List.of("admin", "user")),
                        validations(
                                validation("length", Map.of("min", 3, "max", 255)),
                                validation("username-prohibited-characters", Map.of()),
                                validation("up-username-not-idn-homograph", Map.of())
                        )
                ),
                userProfileAttribute(
                        "email",
                        "${email}",
                        permissions(List.of("admin", "user"), List.of("admin", "user")),
                        validations(
                                validation("email", Map.of()),
                                validation("length", Map.of("max", 255))
                        )
                ),
                userProfileAttribute(
                        "firstName",
                        "${firstName}",
                        permissions(List.of("admin", "user"), List.of("admin", "user")),
                        validations(
                                validation("length", Map.of("max", 255)),
                                validation("person-name-prohibited-characters", Map.of())
                        )
                ),
                userProfileAttribute(
                        "lastName",
                        "${lastName}",
                        permissions(List.of("admin", "user"), List.of("admin", "user")),
                        validations(
                                validation("length", Map.of("max", 255)),
                                validation("person-name-prohibited-characters", Map.of())
                        )
                ),
                userProfileAttribute(
                        WORKSPACE_UUID_ATTRIBUTE,
                        "Workspace UUID",
                        permissions(List.of("admin"), List.of("admin")),
                        validations(validation("length", Map.of("min", 1, "max", 255)))
                ),
                userProfileAttribute(
                        USER_UUID_ATTRIBUTE,
                        "User UUID",
                        permissions(List.of("admin"), List.of("admin")),
                        validations(validation("length", Map.of("min", 1, "max", 255)))
                )
        ));
        payload.put("groups", List.of());
        return payload;
    }

    private Map<String, Object> userProfileAttribute(String name, String displayName,
                                                     Map<String, Object> permissions,
                                                     Map<String, Object> validations) {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("name", name);
        attribute.put("displayName", displayName);
        attribute.put("permissions", permissions);
        attribute.put("validations", validations);
        return attribute;
    }

    private Map<String, Object> permissions(List<String> view, List<String> edit) {
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("view", view);
        permissions.put("edit", edit);
        return permissions;
    }

    @SafeVarargs
    private final Map<String, Object> validations(Map.Entry<String, Object>... validations) {
        Map<String, Object> validationMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> validation : validations) {
            validationMap.put(validation.getKey(), validation.getValue());
        }
        return validationMap;
    }

    private Map.Entry<String, Object> validation(String name, Object value) {
        return Map.entry(name, value);
    }

}
