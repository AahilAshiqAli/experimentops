package com.experimentops.gateway.model.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;

@Getter
@Setter
public class JwsCert {
    private static final Logger log = LoggerFactory.getLogger(JwsCert.class);

    private List<JwsKey> keys;

    public PublicKey getPublicKey(String kid) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        return keys.stream()
                .filter(key -> StringUtils.equalsIgnoreCase(key.getKid(), kid))
                .findFirst()
                .map(this::toPublicKey)
                .orElse(null);
    }

    private PublicKey toPublicKey(JwsKey key) {
        if (!StringUtils.equalsIgnoreCase(key.getKty(), "RSA")) {
            return null;
        }
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.getN()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.getE()));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception ex) {
            log.warn("Unable to parse JWT public key", ex);
            return null;
        }
    }
}
