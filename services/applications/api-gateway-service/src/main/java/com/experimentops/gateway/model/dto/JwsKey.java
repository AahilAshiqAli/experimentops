package com.experimentops.gateway.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwsKey {
    private String kid;
    private String kty;
    private String alg;
    private String n;
    private String e;

}
