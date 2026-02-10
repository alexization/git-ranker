package com.gitranker.api.global.logging;

import lombok.Getter;

@Getter
public enum Event {

    // 사용자 활동
    USER_REGISTERED("신규 사용자 등록 완료", Category.USER),
    USER_LOGIN("로그인 성공", Category.USER),
    USER_REFRESH_REQUESTED("수동 갱신 요청", Category.USER),
    USER_DELETED("사용자 계정 삭제", Category.USER),
    PROFILE_VIEWED("프로필 조회", Category.USER),
    BADGE_VIEWED("배지 조회", Category.USER),

    // 인증/보안
    TOKEN_REFRESHED("토큰 갱신", Category.AUTH),
    AUTH_FAILED("인증 실패", Category.AUTH),
    LOGOUT("로그아웃", Category.AUTH),

    // 배치
    BATCH_STARTED("배치 시작", Category.BATCH),
    BATCH_COMPLETED("배치 완료", Category.BATCH),
    BATCH_FAILED("배치 실패", Category.BATCH),
    BATCH_ITEM_FAILED("배치 항목 실패", Category.BATCH),

    // 외부 API
    GITHUB_API_CALLED("GitHub API 호출", Category.EXTERNAL_API),
    RATE_LIMIT_WARNING("Rate Limit 경고", Category.EXTERNAL_API),

    // HTTP
    HTTP_RESPONSE("HTTP 응답", Category.HTTP),

    // 에러
    ERROR_HANDLED("에러 처리", Category.ERROR);

    private final String description;
    private final Category category;

    Event(String description, Category category) {
        this.description = description;
        this.category = category;
    }

    public enum Category {
        USER, AUTH, BATCH, EXTERNAL_API, HTTP, ERROR
    }
}
