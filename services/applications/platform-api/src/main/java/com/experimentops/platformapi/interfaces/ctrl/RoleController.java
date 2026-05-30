package com.experimentops.platformapi.interfaces.ctrl;

import com.experimentops.platformapi.service.RoleService;
import com.experimentops.utils.HeaderUtil;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.api.v1.RoleApi;
import com.experimentops.workspace.model.v1.RoleModel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class RoleController implements RoleApi {
    private final HttpServletRequest exchange;
    private final RoleService roleService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoleModel>> getRoleByName() {
        ExperimentOpsHeaders experimentOpsHeaders = HeaderUtil.getHeaders(exchange);
        return ResponseEntity.ok(roleService.getRoles(experimentOpsHeaders));
    }
}
