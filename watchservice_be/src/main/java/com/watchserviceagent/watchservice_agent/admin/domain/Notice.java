package com.watchserviceagent.watchservice_agent.admin.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * 클래스 이름 : Notice
 * 기능 : 공지사항 도메인 엔티티.
 */
@Getter
@Builder
@ToString
public class Notice {

    private final Long id;
    private final String title;
    private final String content;
    private final Instant createdAt;
}

