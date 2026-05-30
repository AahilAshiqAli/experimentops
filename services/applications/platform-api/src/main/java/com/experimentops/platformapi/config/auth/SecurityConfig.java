package com.experimentops.platformapi.config.auth;

import com.experimentops.authorization.web.security.CustomUserDetailsService;
import com.experimentops.authorization.web.security.SecurityConfigAuth;
import com.experimentops.authorization.web.security.TokenAuthenticationFilter;
import com.experimentops.authorization.web.security.WebSecurityConfigurerAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityConfigAuth securityConfigAuth;
    private final PasswordEncoder passwordEncoder;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;


    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        securityConfigAuth.configure(authenticationManagerBuilder, customUserDetailsService, passwordEncoder);
    }

    @Override
    public void configure(WebSecurity web) {
        securityConfigAuth.configure(web);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        securityConfigAuth.configure(http, tokenAuthenticationFilter);
    }
}
