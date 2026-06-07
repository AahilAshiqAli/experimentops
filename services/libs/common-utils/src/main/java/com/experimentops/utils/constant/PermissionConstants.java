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
    public static final String ADD_WORKSPACE = WORKSPACE + ADD;
    public static final String ADD_DATASET = DATASET + ADD;


}
