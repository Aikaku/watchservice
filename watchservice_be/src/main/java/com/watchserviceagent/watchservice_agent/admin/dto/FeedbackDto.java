package com.watchserviceagent.watchservice_agent.admin.dto;

import com.watchserviceagent.watchservice_agent.admin.domain.Feedback;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 클래스 이름 : FeedbackDto
 * 기능 : 관리자/프론트로 내려보내는 피드백 응답 DTO.
 */
@Getter
@Builder
@ToString
public class FeedbackDto {

    private final long id;
    private final String email;
    private final String content;
    private final String createdAt;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static FeedbackDto from(Feedback f) {
        return FeedbackDto.builder()
                .id(f.getId())
                .email(f.getEmail())
                .content(f.getContent())
                .createdAt(FMT.format(f.getCreatedAt()))
                .build();
    }
}

