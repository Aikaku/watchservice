package com.watchserviceagent.watchservice_agent.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 클래스 이름 : GuideService
 * 기능 : 사용 가이드 조회/수정 비즈니스 로직을 제공한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;

    public String getContent() {
        return guideRepository.findContent();
    }

    public void updateContent(String content) {
        guideRepository.upsertContent(content);
    }
}
