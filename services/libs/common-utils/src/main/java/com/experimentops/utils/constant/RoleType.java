package com.experimentops.utils.constant;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum RoleType {

    PLATFORM_ADMIN("PLATFORM_ADMIN", List.of(
    ), true),
    WORKSPACE_ADMIN("WORKSPACE_ADMIN", List.of(
            PermissionConstants.ADD_USER
    ), false),
    RESEARCHER("RESEARCHER", List.of(
    ), false);

    private final String roleName;
    private final List<String> permissions;
    private final boolean isInternal;

    // all roles by name
    public static final Map<String, RoleType> roleTypeMap = new HashMap<>();

    static {
        for (RoleType roleType : RoleType.values()) {
            roleTypeMap.put(roleType.roleName, roleType);
        }
    }

    // internal roles only
    public static final Map<String, RoleType> internalRoleTypeMap = new HashMap<>();

    static {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.isInternal) {
                internalRoleTypeMap.put(roleType.roleName, roleType);
            }
        }
    }

    RoleType(String roleName, List<String> permissions, boolean isInternal) {
        this.roleName = roleName;
        this.permissions = permissions;
        this.isInternal = isInternal;
    }

}