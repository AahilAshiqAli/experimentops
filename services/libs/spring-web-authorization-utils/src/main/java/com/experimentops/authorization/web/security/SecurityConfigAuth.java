package com.experimentops.authorization.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Not the main security config. If we plan to create spring-boot worker then this is just a utility which would do most of the work of future SecurityConfig file
@Component
public class SecurityConfigAuth {
    @Value("${experimentops.security.internal-authenticated-paths:}")
    private String internalAuthenticatedPaths;

    // Registers a custom JWT/token filter that intercepts every request and validates the token before it reaches your controllers.
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(CustomUserDetailsService customUserDetailsService) {
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(customUserDetailsService);
        filter.excludePathFromFilter("/actuator/");
        filter.internalAuthenticatedUrl("/actuator/");
        filter.internalAuthenticatedUrls(csvToSet(internalAuthenticatedPaths));
        return filter;
    }

    // Standard BCrypt encoder — used when creating/verifying passwords.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder,
                          CustomUserDetailsService customUserDetailsService,
                          PasswordEncoder passwordEncoder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);
    }

    public void configure(WebSecurity web) {
        web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**");
    }

    public void configure(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .sessionManagement(t -> t.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .authorizeHttpRequests(r -> r
                        .requestMatchers("/", "/error", "/favicon.ico", "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    private Set<String> csvToSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .collect(Collectors.toSet());
    }
}
