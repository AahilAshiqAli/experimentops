package com.experimentops.platformapi.config.auth;

import com.experimentops.authorization.web.security.CustomUserDetailsService;
import com.experimentops.authorization.web.security.SecurityConfigAuth;
import com.experimentops.authorization.web.security.TokenAuthenticationFilter;
import com.experimentops.authorization.web.security.WebSecurityConfigurerAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityConfigAuth securityConfigAuth;
    private final PasswordEncoder passwordEncoder;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    //Added this because did not want to use PostConfig
    public SecurityConfig(CustomUserDetailsService customUserDetailsService, SecurityConfigAuth securityConfigAuth,
                          PasswordEncoder passwordEncoder, TokenAuthenticationFilter tokenAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.securityConfigAuth = securityConfigAuth;
        this.passwordEncoder = passwordEncoder;
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.tokenAuthenticationFilter.internalAuthenticatedUrl("/auth/login");
        this.tokenAuthenticationFilter.internalAuthenticatedUrl("/auth/reset-password/verify");
        this.tokenAuthenticationFilter.internalAuthenticatedUrl("/auth/forgot-password/generate");
    }

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
