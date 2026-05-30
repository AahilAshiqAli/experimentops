package com.experimentops.platformapi.transformer;

import com.experimentops.workspace.event.WorkspaceMutationEvent;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeyCloakTransformer {

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

    private static final String WORKSPACE_UUID_ATTRIBUTE = "workspace_uuid";
    private static final String USER_UUID_ATTRIBUTE = "user_uuid";

    private static final String OFFLINE_ACCESS = "offline_access";
    private static final List<String> DEFAULT_ROLES = List.of(OFFLINE_ACCESS, "uma_authorization");

    public RealmRepresentation transformRealm(String realmName, WorkspaceMutationEvent workspaceMutationEvent) {

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setEnabled(true);
        realmRepresentation.setRealm(realmName);
        realmRepresentation.setDisplayName(realmName);

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
        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setName("rsa");
        componentRepresentation.setProviderId("rsa");
        componentRepresentation.setProviderType("org.keycloak.keys.KeyProvider");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.put("priority", Collections.singletonList("101"));
        config.put("enabled", Collections.singletonList("true"));
        config.put("active", Collections.singletonList("true"));
        config.put("algorithm", Collections.singletonList("RS256"));
        config.put("privateKey", Collections.singletonList(rsaKey));

        componentRepresentation.setConfig(config);

        return componentRepresentation;
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
}
