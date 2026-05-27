package com.experimentops.common.kafka.constant;

public final class KafkaConstants {

    private KafkaConstants() {
        /* This utility class should not be instantiated */
    }

    public static final String ERROR_CODE = "EXPERIMENTOPS-ERROR-CODE";
    public static final String EXCEPTION_MESSAGE = "EXPERIMENTOPS-EXCEPTION-MESSAGE";
    public static final String PUBLISHER_CLASS_NAME = "EXPERIMENTOPS-PUBLISHER-CLASS-NAME";
    public static final String PUBLISHER_CLASS = PUBLISHER_CLASS_NAME;

    public static final String REQUEST_UUID = "EXPERIMENTOPS-REQUEST-UUID";
    public static final String WORKSPACE_UUID = "EXPERIMENTOPS-WORKSPACE-UUID";

    public static final String NOT_AVAILABLE = "N/A";
}
