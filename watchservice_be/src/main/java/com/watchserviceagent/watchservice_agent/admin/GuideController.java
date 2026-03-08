package com.watchserviceagent.watchservice_agent.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 클래스 이름 : GuideController
 * 기능 : 사용 가이드 조회(공개) 및 수정(관리자 전용) API를 제공한다.
 *   GET  /api/guide          — 사용자 가이드 조회 (인증 불필요)
 *   PUT  /api/admin/guide    — 가이드 수정 (관리자 세션 필요)
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class GuideController {

    private final GuideService guideService;

    /**
     * 사용자/공개: 가이드 내용 조회
     * GET /api/guide
     */
    @GetMapping("/api/guide")
    public Map<String, String> getGuide() {
        return Map.of("content", guideService.getContent());
    }

    /**
     * 관리자: 가이드 내용 수정
     * PUT /api/admin/guide
     */
    @PutMapping("/api/admin/guide")
    public ResponseEntity<?> updateGuide(@RequestBody GuideRequest req) {
        if (req.getContent() == null) {
            return ResponseEntity.badRequest().body("content is required");
        }
        guideService.updateContent(req.getContent());
        return ResponseEntity.ok().build();
    }

    @Getter
    @Setter
    public static class GuideRequest {
        private String content;
    }
}
