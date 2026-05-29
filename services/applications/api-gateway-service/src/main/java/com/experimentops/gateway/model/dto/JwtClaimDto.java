package com.experimentops.gateway.model.dto;

import java.util.Date;

public class JwtClaimDto {
    private String iss;
    private String name;
    private Date exp;
    private Date nbf;
    private String workspaceUuid;
    private String userUuid;
    private String role;

    public String getIss() { return iss; }
    public void setIss(String iss) { this.iss = iss; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Date getExp() { return exp; }
    public void setExp(Date exp) { this.exp = exp; }
    public Date getNbf() { return nbf; }
    public void setNbf(Date nbf) { this.nbf = nbf; }
    public String getWorkspaceUuid() { return workspaceUuid; }
    public void setWorkspaceUuid(String workspaceUuid) { this.workspaceUuid = workspaceUuid; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
