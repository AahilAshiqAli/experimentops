package com.experimentops.gateway.util;

import com.experimentops.gateway.model.dto.JwtClaimDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtVerifier {
    public boolean verify(JwtClaimDto claimDto) {
        if (claimDto == null) {
            return false;
        }
        Date now = new Date();
        Date exp = claimDto.getExp();
        if (exp != null && exp.before(now)) {
            return false;
        }
        Date nbf = claimDto.getNbf();
        if (nbf != null && nbf.after(now)) {
            return false;
        }
        return StringUtils.isNotBlank(claimDto.getRole());
    }
}
