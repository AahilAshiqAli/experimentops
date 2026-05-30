package com.experimentops.authorization.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// a wrapper around Spring's SecurityContextHolder to make accessing the current authenticated user cleaner and more testable.
// We cannot mock SecurityContextHolder directly in tests so we need a wrapper around for that.

@Component
public class AuthenticationFacadeImpl implements AuthenticationFacade {
    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public String getCurrentUser() {
        return getUserPrincipal().map(UserPrincipal::getUsername).orElse(null);
    }

    @Override
    public Optional<UserPrincipal> getUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public List<String> getPermissions() {
        return getUserPrincipal().map(UserPrincipal::getActions).orElseGet(ArrayList::new);
    }
}
