package com.experimentops.utils.constant;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum PermissionType {

    ADD_USER(PermissionConstants.ADD_USER, "Add User"),
    ADD_WORKSPACE(PermissionConstants.ADD_WORKSPACE, "Add Workspace");

    private final String code;
    private final String name;
    public static final Map<String, PermissionType> permissionMap = new HashMap<>();

    PermissionType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    static {
        for (PermissionType type : PermissionType.values()) {
            permissionMap.put(type.code, type);
        }
    }
}
