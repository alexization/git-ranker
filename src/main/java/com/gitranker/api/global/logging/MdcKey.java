package com.gitranker.api.global.logging;

public class MdcKey {
    public static final String TRACE_ID = "trace_id";
    public static final String USERNAME = "username";
    public static final String NODE_ID = "node_id";
    public static final String CLIENT_IP = "client_ip";
    public static final String METHOD = "method";
    public static final String URI = "uri";
    public static final String HTTP_STATUS = "http_status";
    public static final String LATENCY_MS = "latency_ms";
    public static final String DB_QUERY_TIME_MS = "db_query_time_ms";
    public static final String Github_API_CALL_TIME_MS = "github_api_call_time_ms";
    public static final String JOB_NAME = "job_name";
    public static final String JOB_ID = "job_id";
    public static final String STEP_NAME = "step_name";
    public static final String CHUNK_NUMBER = "chunk_number";
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String ERROR_HASH = "error_hash";

    private MdcKey() {
        throw new AssertionError("유틸 클래스는 인스턴스화 할 수 없습니다.");
    }
}
