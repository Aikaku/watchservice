package com.watchserviceagent.watchservice_agent.admin.dto;

import com.watchserviceagent.watchservice_agent.admin.domain.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 클래스 이름 : NoticeDto
 * 기능 : 공지사항 응답 DTO.
 */
@Getter
@Builder
@ToString
public class NoticeDto {

    private final long id;
    private final String title;
    private final String content;
    private final String createdAt;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /*
     * 함수 이름 : from
     * 기능 : Notice 도메인 객체를 NoticeDto로 변환한다.
     * 매개변수 : n - 변환할 Notice 객체
     * 반환값 : NoticeDto - 변환된 DTO 객체
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public static NoticeDto from(Notice n) {
        return NoticeDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .createdAt(FMT.format(n.getCreatedAt()))
                .build();
    }
}

