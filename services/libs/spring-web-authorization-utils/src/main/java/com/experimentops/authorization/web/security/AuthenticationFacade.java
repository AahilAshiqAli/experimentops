package com.experimentops.authorization.web.security;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

public interface AuthenticationFacade {
    Authentication getAuthentication();

    String getCurrentUser();

    Optional<UserPrincipal> getUserPrincipal();

    List<String> getPermissions();
}
