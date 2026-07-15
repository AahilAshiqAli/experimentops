package com.experimentops.utils.constant;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum PermissionType {

    ADD_USER(PermissionConstants.ADD_USER, "Add User"),
    GET_USER(PermissionConstants.GET_USER, "Get User"),
    ADD_WORKSPACE(PermissionConstants.ADD_WORKSPACE, "Add Workspace"),
    ADD_DATASET(PermissionConstants.ADD_DATASET, "Add Dataset"),
    EDIT_DATASET(PermissionConstants.EDIT_DATASET, "Edit Dataset"),
    GET_DATASET(PermissionConstants.GET_DATASET, "Get Dataset"),
    ADD_PROJECT(PermissionConstants.ADD_PROJECT, "Add Project"),
    EDIT_PROJECT(PermissionConstants.EDIT_PROJECT, "Edit Project"),
    GET_PROJECT(PermissionConstants.GET_PROJECT, "Get Project"),
    ADD_EXPERIMENT(PermissionConstants.ADD_EXPERIMENT, "Add Experiment"),
    EDIT_EXPERIMENT(PermissionConstants.EDIT_EXPERIMENT, "Edit Experiment"),
    GET_EXPERIMENT(PermissionConstants.GET_EXPERIMENT, "Get Experiment"),
    RUN_EXPERIMENT(PermissionConstants.RUN_EXPERIMENT, "Run Experiment"),
    ADD_EXPERIMENT_CONFIG(PermissionConstants.ADD_EXPERIMENT_CONFIG, "Add Experiment Config"),
    EDIT_EXPERIMENT_CONFIG(PermissionConstants.EDIT_EXPERIMENT_CONFIG, "Edit Experiment Config"),
    GET_EXPERIMENT_CONFIG(PermissionConstants.GET_EXPERIMENT_CONFIG, "Get Experiment Config"),
    ADD_EXPERIMENT_TYPE(PermissionConstants.ADD_EXPERIMENT_TYPE, "Add Experiment Type"),
    EDIT_EXPERIMENT_TYPE(PermissionConstants.EDIT_EXPERIMENT_TYPE, "Edit Experiment Type"),
    GET_EXPERIMENT_TYPE(PermissionConstants.GET_EXPERIMENT_TYPE, "Get Experiment Type"),
    GET_EXPERIMENT_RUNS(PermissionConstants.GET_EXPERIMENT_RUNS, "Get Experiment Runs");


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
