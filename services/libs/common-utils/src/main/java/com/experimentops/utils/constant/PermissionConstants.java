package com.experimentops.utils.constant;

public final class PermissionConstants {

    private PermissionConstants() {
        /* This is a utility class and cannot be instantiated */
    }

    private static final String ADD = ":add";
    private static final String EDIT = ":edit";
    private static final String GET = ":get";
    private static final String USER = ":user";
    private static final String WORKSPACE = ":workspace";
    private static final String DATASET = ":dataset";
    private static final String PROJECT = ":project";
    private static final String RUN = ":run";
    private static final String EXPERIMENT = ":experiment";
    private static final String EXPERIMENT_TYPE = ":experiment-type";
    public static final String ADD_USER = USER + ADD;
    public static final String ADD_WORKSPACE = WORKSPACE + ADD;
    public static final String ADD_DATASET = DATASET + ADD;
    public static final String EDIT_DATASET = DATASET + EDIT;
    public static final String GET_DATASET = DATASET + GET;
    public static final String ADD_PROJECT = PROJECT + ADD;
    public static final String EDIT_PROJECT = PROJECT + EDIT;
    public static final String GET_PROJECT = PROJECT + GET;
    public static final String ADD_EXPERIMENT = EXPERIMENT + ADD;
    public static final String EDIT_EXPERIMENT = EXPERIMENT + EDIT;
    public static final String GET_EXPERIMENT = EXPERIMENT + GET;
    public static final String RUN_EXPERIMENT = EXPERIMENT + RUN;
    public static final String ADD_EXPERIMENT_TYPE = EXPERIMENT_TYPE + ADD;
    public static final String EDIT_EXPERIMENT_TYPE = EXPERIMENT_TYPE + EDIT;
    public static final String GET_EXPERIMENT_TYPE = EXPERIMENT_TYPE + GET;


}
