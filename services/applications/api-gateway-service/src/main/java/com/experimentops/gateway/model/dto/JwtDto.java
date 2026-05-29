package com.experimentops.gateway.model.dto;

public class JwtDto {
    private JwtClaimDto claim;

    public JwtDto(JwtClaimDto claim) {
        this.claim = claim;
    }

    public JwtClaimDto getClaim() {
        return claim;
    }
}
