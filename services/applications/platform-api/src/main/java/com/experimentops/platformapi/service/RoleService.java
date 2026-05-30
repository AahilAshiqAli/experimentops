package com.experimentops.platformapi.service;

import com.experimentops.platformapi.transformer.RoleTransformer;
import com.experimentops.utils.constant.RoleType;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.experimentops.workspace.model.v1.RoleModel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleTransformer roleTransformer;
    private static final Set<RoleType> VALID_ROLES = Set.of(
            RoleType.RESEARCHER, RoleType.WORKSPACE_ADMIN
    );

    @NonNull
    public List<RoleModel> getRoles(@NonNull ExperimentOpsHeaders experimentOpsHeaders) {
        RoleType role = RoleType.roleTypeMap.get(experimentOpsHeaders.getUserRole());
        List<RoleModel> roleModels = new ArrayList<>();
        if (role != null && VALID_ROLES.contains(role)) {
            roleModels.add(roleTransformer.mapToRoleModel(role));
        }
        return roleModels;
    }
}
