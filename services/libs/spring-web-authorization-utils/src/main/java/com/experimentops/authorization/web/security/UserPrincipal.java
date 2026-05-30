package com.experimentops.authorization.web.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<String> actions;

    public UserPrincipal(String role, Collection<? extends GrantedAuthority> authorities, List<String> actions) {
        this.role = role;
        this.authorities = authorities;
        this.actions = actions;
    }

    public static UserPrincipal create(String role, List<String> actions) {
        List<GrantedAuthority> authorities = actions.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return new UserPrincipal(role, authorities, actions);
    }

    @Override
    public String getPassword() {
        return getRole();
    }

    @Override
    public String getUsername() {
        return getRole();
    }
}
