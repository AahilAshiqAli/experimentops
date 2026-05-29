package com.experimentops.gateway.util;

import com.experimentops.gateway.model.dto.JwtDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtVerifier {
    public boolean verify(JwtDto jwtDto) {
        if (jwtDto == null || jwtDto.getClaim() == null) {
            return false;
        }
        Date now = new Date();
        Date exp = jwtDto.getClaim().getExp();
        if (exp != null && exp.before(now)) {
            return false;
        }
        Date nbf = jwtDto.getClaim().getNbf();
        if (nbf != null && nbf.after(now)) {
            return false;
        }
        return StringUtils.isNotBlank(jwtDto.getClaim().getRole());
    }
}
