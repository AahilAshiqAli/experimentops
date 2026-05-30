package com.experimentops.gateway.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JwtClaimDto {
    private String iss;
    private String name;
    private Date exp;
    private Date nbf;
    private String workspaceUuid;
    private String userUuid;
    private String role;

}
