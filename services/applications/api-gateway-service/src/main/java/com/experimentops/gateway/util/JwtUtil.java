package com.experimentops.gateway.util;

import com.experimentops.gateway.model.dto.JwsCert;
import com.experimentops.gateway.model.dto.JwtClaimDto;
import com.experimentops.gateway.model.dto.JwtDto;
import com.experimentops.utils.JSONUtil;
import com.experimentops.utils.constant.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String BEARER = "Bearer ";

    private final JwtCertLoader jwtCertLoader;
    private final JwtVerifier jwtVerifier;

    @Value("${jwks.clock.skew.seconds:0}")
    private long clockSkewSeconds;

    @Value("${jwks.client-id:}")
    private String clientId;

    private JwsCert jwsCert;


    public JwtDto parseAuthorization(String authorization) {
        if (StringUtils.length(authorization) > BEARER.length() && StringUtils.startsWithIgnoreCase(authorization, BEARER)) {
            authorization = StringUtils.trimToNull(StringUtils.substring(authorization, BEARER.length()));
        }
        if (StringUtils.isBlank(authorization)) {
            return null;
        }

        loadCerts();
        if (jwsCert == null) {
            log.warn("JWKS certs are not loaded");
            return null;
        }

        Jws<Claims> claims;
        try {
            claims = Jwts.parser()
                    .clockSkewSeconds(clockSkewSeconds)
                    .keyLocator(header -> {
                        Object kidHeader = header.get("kid");
                        return kidHeader instanceof String kid ? jwsCert.getPublicKey(kid) : null;
                    })
                    .build()
                    .parseSignedClaims(authorization);
        } catch (Exception ex) {
            log.warn("Unable to verify JWT", ex);
            return null;
        }

        JwtClaimDto claimDto = new JwtClaimDto();
        Claims payload = claims.getPayload();
        claimDto.setIss(payload.getIssuer());
        claimDto.setName(StringUtils.defaultIfBlank(payload.get("preferred_username", String.class), payload.getSubject()));
        claimDto.setExp(payload.getExpiration());
        claimDto.setNbf(payload.getNotBefore());
        claimDto.setWorkspaceUuid(payload.get("workspace_uuid", String.class));
        claimDto.setUserUuid(StringUtils.defaultIfBlank(payload.get("user_uuid", String.class), payload.getSubject()));
        claimDto.setRole(extractRole(payload));

        JwtDto jwtDto = new JwtDto(claimDto);
        return jwtVerifier.verify(jwtDto) ? jwtDto : null;
    }

    private void loadCerts() {
        if (jwsCert != null) {
            return;
        }
        String json = jwtCertLoader.loadCerts();
        if (StringUtils.isNotBlank(json)) {
            jwsCert = JSONUtil.toObjectFromTypedJson(json, JwsCert.class);
        }
    }

    private String extractRole(Claims claims) {
        String explicitRole = claims.get("role", String.class);
        if (StringUtils.isNotBlank(explicitRole)) {
            return explicitRole;
        }

        Object roles = claims.get("roles");
        String matchedRole = findKnownRole(roles);
        if (matchedRole != null) {
            return matchedRole;
        }

        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            matchedRole = findKnownRole(realmAccessMap.get("roles"));
            if (matchedRole != null) {
                return matchedRole;
            }
        }

        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
            if (StringUtils.isNotBlank(clientId)) {
                matchedRole = findKnownRoleFromClient(resourceAccessMap.get(clientId));
                if (matchedRole != null) {
                    return matchedRole;
                }
            }
            for (Object clientAccess : resourceAccessMap.values()) {
                matchedRole = findKnownRoleFromClient(clientAccess);
                if (matchedRole != null) {
                    return matchedRole;
                }
            }
        }

        return null;
    }

    private String findKnownRoleFromClient(Object clientAccess) {
        if (clientAccess instanceof Map<?, ?> clientAccessMap) {
            return findKnownRole(clientAccessMap.get("roles"));
        }
        return null;
    }

    private String findKnownRole(Object roles) {
        if (roles instanceof List<?> roleList) {
            for (Object role : roleList) {
                if (role instanceof String roleName && RoleType.roleTypeMap.containsKey(roleName)) {
                    return roleName;
                }
            }
        }
        if (roles instanceof String roleName && RoleType.roleTypeMap.containsKey(roleName)) {
            return roleName;
        }
        return null;
    }
}
