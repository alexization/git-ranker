package com.gitranker.api.global.logging;

import lombok.Getter;

@Getter
public enum Event {

    USER_REGISTERED("신규 사용자 등록 완료"),
    USER_LOGIN("로그인 성공"),
    USER_REFRESH_REQUESTED("수동 갱신 요청"),
    USER_DELETED("사용자 계정 삭제"),
    PROFILE_VIEWED("프로필 조회"),
    BADGE_VIEWED("배지 조회"),

    BATCH_COMPLETED("배치 완료"),
    BATCH_ITEM_FAILED("배치 항목 실패"),

    GITHUB_API_CALLED("GitHub API 호출"),
    HTTP_RESPONSE("HTTP 응답"),
    RATE_LIMIT_WARNING("Rate Limit 경고");

    private final String description;

    Event(String description) {
        this.description = description;
    }

}
