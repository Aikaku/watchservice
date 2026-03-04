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

    public static NoticeDto from(Notice n) {
        return NoticeDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .createdAt(FMT.format(n.getCreatedAt()))
                .build();
    }
}

