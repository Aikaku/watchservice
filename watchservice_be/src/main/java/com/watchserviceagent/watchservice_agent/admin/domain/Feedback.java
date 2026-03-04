package com.watchserviceagent.watchservice_agent.admin.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * 클래스 이름 : Feedback
 * 기능 : 사용자 피드백 도메인 엔티티.
 */
@Getter
@Builder
@ToString
public class Feedback {

    private final Long id;
    private final String email;
    private final String content;
    private final Instant createdAt;
}

