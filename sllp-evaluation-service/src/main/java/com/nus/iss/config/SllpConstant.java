package com.nus.iss.config;

import java.math.BigDecimal;

public final class SllpConstant {
    public static final String JSON_RPC_VERSION = "2.0";
    public static final String SEND_MESSAGE = "SendMessage";
    public static final String JSON_MEDIA_TYPE = "application/json";
    public static final String COMPLETED_STATE = "TASK_STATE_COMPLETED";
    public static final String PROMPT_VERSION = "grading-v4";
    public static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    public static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private SllpConstant() {
    }
}
