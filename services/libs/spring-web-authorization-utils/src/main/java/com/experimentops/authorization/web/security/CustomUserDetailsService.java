package com.experimentops.authorization.web.security;

import com.experimentops.utils.constant.RoleType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String role) throws UsernameNotFoundException {
        RoleType roleType = RoleType.roleTypeMap.get(role);
        if (roleType == null) {
            throw new UsernameNotFoundException("Unknown role: " + role);
        }
        return UserPrincipal.create(roleType.getRoleName(), roleType.getPermissions());
    }
}
