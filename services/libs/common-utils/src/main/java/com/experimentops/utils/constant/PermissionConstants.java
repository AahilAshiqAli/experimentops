package com.experimentops.utils.constant;

public final class PermissionConstants {

    private PermissionConstants() {
        /* This is a utility class and cannot be instantiated */
    }

    private static final String ADD = ":add";
    private static final String EDIT = ":edit";
    private static final String DELETE = ":delete";
    private static final String GET = ":get";
    private static final String USER = ":user";
    public static final String ADD_USER = USER + ADD;
    private static final String WORKSPACE = ":workspace";
    private static final String DATASET = ":dataset";
    private static final String PROJECT = ":project";
    public static final String ADD_WORKSPACE = WORKSPACE + ADD;
    public static final String ADD_DATASET = DATASET + ADD;
    public static final String ADD_PROJECT = PROJECT + ADD;
    public static final String EDIT_PROJECT = PROJECT + EDIT;
    public static final String GET_PROJECT = PROJECT + GET;
    public static final String DELETE_PROJECT = PROJECT + DELETE;


}
