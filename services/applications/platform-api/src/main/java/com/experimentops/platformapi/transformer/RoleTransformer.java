package com.experimentops.platformapi.transformer;

import com.experimentops.utils.constant.PermissionType;
import com.experimentops.utils.constant.RoleType;
import com.experimentops.workspace.model.v1.PermissionModel;
import com.experimentops.workspace.model.v1.RoleModel;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RoleTransformer {
    @NonNull
    public RoleModel mapToRoleModel(@NonNull RoleType role) {
        RoleModel roleModel = new RoleModel();
        roleModel.setRoleName(role.getRoleName());
        roleModel.setPermissionList(role.getPermissions().stream()
                .map(this::mapToPermissionModel)
                .toList());
        return roleModel;
    }

    @NonNull
    private PermissionModel mapToPermissionModel(@NonNull String permission) {
        PermissionType type = PermissionType.permissionMap.get(permission);
        PermissionModel permissionModel = new PermissionModel();
        permissionModel.setPermissionName(type.getName());
        permissionModel.setCode(type.getCode());
        return permissionModel;
    }
}
