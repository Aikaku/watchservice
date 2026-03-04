package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.dto.FeedbackDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클래스 이름 : FeedbackController
 * 기능 : 사용자 피드백 저장 및 관리자 피드백 조회/삭제 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 사용자 피드백 저장
     * POST /api/feedback
     */
    @PostMapping("/api/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }
        feedbackService.saveUserFeedback(req.getEmail(), req.getContent());
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 피드백 조회
     * GET /api/admin/feedback
     */
    @GetMapping("/api/admin/feedback")
    public List<FeedbackDto> getFeedbackForAdmin() {
        return feedbackService.getAllForAdmin();
    }

    /**
     * 관리자 피드백 삭제
     * DELETE /api/admin/feedback/{id}
     */
    @DeleteMapping("/api/admin/feedback/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable long id) {
        int deleted = feedbackService.deleteById(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Setter
    public static class FeedbackRequest {
        private String email;
        private String content;
    }
}

