package com.gitranker.api.global.logging;

public class MdcKey {

    public static final String LOG_CATEGORY = "log_category";
    public static final String EVENT_TYPE = "event_type";

    /* HTTP 요청 관련 */
    public static final String TRACE_ID = "trace_id";
    public static final String CLIENT_IP = "client_ip";
    public static final String HTTP_METHOD = "http_method";
    public static final String REQUEST_URI = "request_uri";
    public static final String HTTP_STATUS = "http_status";
    public static final String LATENCY_MS = "latency_ms";

    /* 사용자 관련 */
    public static final String USERNAME = "username";
    public static final String NODE_ID = "node_id";

    /* 배치 관련 */
    public static final String JOB_NAME = "job_name";
    public static final String STEP_NAME = "step_name";

    /* 인프라 및 외부 API 관련 */
    public static final String DB_QUERY_TIME_MS = "db_query_time_ms";
    public static final String GITHUB_API_CALL_TIME_MS = "github_api_call_time_ms";
    public static final String GITHUB_API_COST =  "github_api_cost";
    public static final String GITHUB_API_REMAINING = "github_api_remaining";
    public static final String GITHUB_API_RESET_AT = "github_api_reset_at";

    /* 에러 관련 */
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";

    private MdcKey() {
        throw new AssertionError("유틸 클래스는 인스턴스화 할 수 없습니다.");
    }
}
