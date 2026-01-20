package com.gitranker.api.global.logging;

public final class MdcKey {
    public static final String LOG_CATEGORY = "log_category";
    public static final String EVENT_TYPE = "event_type";

    public static final String TRACE_ID = "trace_id";
    public static final String CLIENT_IP = "client_ip";
    public static final String HTTP_METHOD = "http_method";
    public static final String REQUEST_URI = "request_uri";
    public static final String HTTP_STATUS = "http_status";
    public static final String LATENCY_MS = "latency_ms";

    public static final String USERNAME = "username";
    public static final String NODE_ID = "node_id";
    public static final String TIER = "tier";
    public static final String TIER_FROM = "tier_from";
    public static final String TIER_TO = "tier_to";
    public static final String SCORE = "score";
    public static final String SCORE_FROM = "score_from";
    public static final String SCORE_TO = "score_to";
    public static final String RANKING = "ranking";

    public static final String JOB_NAME = "job_name";
    public static final String STEP_NAME = "step_name";
    public static final String BATCH_TOTAL_COUNT = "batch_total_count";
    public static final String BATCH_PROCESSED_COUNT = "batch_processed_count";
    public static final String BATCH_SUCCESS_COUNT = "batch_success_count";
    public static final String BATCH_FAIL_COUNT = "batch_fail_count";
    public static final String BATCH_SKIP_COUNT = "batch_skip_count";
    public static final String BATCH_PROGRESS_PERCENT = "batch_progress_percent";
    public static final String BATCH_DURATION_MS = "batch_duration_ms";

    public static final String GITHUB_API_CALL_TIME_MS = "github_api_call_time_ms";
    public static final String GITHUB_API_COST = "github_api_cost";
    public static final String GITHUB_API_REMAINING = "github_api_remaining";
    public static final String GITHUB_API_RESET_AT = "github_api_reset_at";

    public static final String DB_QUERY_TIME_MS = "db_query_time_ms";

    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";

    public static final String AUTH_METHOD = "auth_method";
    public static final String AUTH_FAILURE_REASON = "auth_failure_reason";

    private MdcKey() {
        throw new AssertionError("유틸 클래스는 인스턴스화 할 수 없습니다.");
    }
}
