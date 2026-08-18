package com.experimentops.utils.constant;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum RoleType {

    PLATFORM_ADMIN("PLATFORM_ADMIN", List.of(
            PermissionConstants.ADD_WORKSPACE,
            PermissionConstants.ADD_USER,
            PermissionConstants.GET_EXPERIMENT_TYPE,
            PermissionConstants.ADD_EXPERIMENT_TYPE,
            PermissionConstants.EDIT_EXPERIMENT_TYPE
    ), true),
    WORKSPACE_ADMIN("WORKSPACE_ADMIN", List.of(
            PermissionConstants.ADD_USER,
            PermissionConstants.GET_USER,
            PermissionConstants.ADD_DATASET,
            PermissionConstants.EDIT_DATASET,
            PermissionConstants.GET_DATASET,
            PermissionConstants.ADD_PROJECT,
            PermissionConstants.EDIT_PROJECT,
            PermissionConstants.GET_PROJECT,
            PermissionConstants.ADD_EXPERIMENT,
            PermissionConstants.EDIT_EXPERIMENT,
            PermissionConstants.GET_EXPERIMENT,
            PermissionConstants.ADD_EXPERIMENT_CONFIG,
            PermissionConstants.EDIT_EXPERIMENT_CONFIG,
            PermissionConstants.GET_EXPERIMENT_CONFIG,
            PermissionConstants.RUN_EXPERIMENT,
            PermissionConstants.GET_EXPERIMENT_TYPE,
            PermissionConstants.GET_EXPERIMENT_RUNS,
            PermissionConstants.ADD_EXPERIMENT_TYPE,
            PermissionConstants.EDIT_EXPERIMENT_TYPE,
            PermissionConstants.COMPARE_EXPERIMENT_RUNS
    ), false),
    RESEARCHER("RESEARCHER", List.of(
            PermissionConstants.ADD_DATASET,
            PermissionConstants.GET_DATASET,
            PermissionConstants.GET_PROJECT,
            PermissionConstants.ADD_EXPERIMENT,
            PermissionConstants.EDIT_EXPERIMENT,
            PermissionConstants.GET_EXPERIMENT,
            PermissionConstants.ADD_EXPERIMENT_CONFIG,
            PermissionConstants.EDIT_EXPERIMENT_CONFIG,
            PermissionConstants.GET_EXPERIMENT_CONFIG,
            PermissionConstants.RUN_EXPERIMENT,
            PermissionConstants.GET_EXPERIMENT_TYPE,
            PermissionConstants.GET_EXPERIMENT_RUNS,
            PermissionConstants.COMPARE_EXPERIMENT_RUNS
    ), false),
    INTERNAL("INTERNAL", List.of(), true);

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

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            RoleType.valueOf(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }



}
