package com.experimentops.platformapi.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KeycloakConfig {

    @Value("${experimentops.keycloak.master.server-url}")
    private String serverUrl;

    @Value("${experimentops.keycloak.master.realm}")
    private String masterRealm;

    @Value("${experimentops.keycloak.master.username}")
    private String masterUsername;

    @Value("${experimentops.keycloak.master.password}")
    private String masterPassword;

    @Value("${experimentops.keycloak.master.client-id}")
    private String masterClientId;

    @Value("${experimentops.keycloak.admin-client.connection-pool-size}")
    private int connectionPoolSize;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(masterRealm)
                .username(masterUsername)
                .password(masterPassword)
                .clientId(masterClientId)
                .grantType(OAuth2Constants.PASSWORD)
                .resteasyClient(new ResteasyClientBuilder()
                        .connectionPoolSize(connectionPoolSize)
                        .build())
                .build();
    }
}
