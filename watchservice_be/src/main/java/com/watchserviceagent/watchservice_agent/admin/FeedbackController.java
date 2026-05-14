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

    /*
     * 함수 이름 : submitFeedback
     * 기능 : 사용자가 제출한 피드백을 저장한다.
     * 매개변수 : req - 피드백 요청 (email, content)
     * 반환값 : 200 OK (성공), 400 Bad Request (content 누락)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @PostMapping("/api/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }
        feedbackService.saveUserFeedback(req.getEmail(), req.getContent());
        return ResponseEntity.ok().build();
    }

    /*
     * 함수 이름 : getFeedbackForAdmin
     * 기능 : 관리자 권한으로 모든 피드백 목록을 조회한다.
     * 매개변수 : 없음
     * 반환값 : List<FeedbackDto> - 피드백 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/api/admin/feedback")
    public List<FeedbackDto> getFeedbackForAdmin() {
        return feedbackService.getAllForAdmin();
    }

    /*
     * 함수 이름 : deleteFeedback
     * 기능 : 관리자 권한으로 특정 ID의 피드백을 삭제한다.
     * 매개변수 : id - 삭제할 피드백 ID
     * 반환값 : 204 No Content (성공), 404 Not Found (없을 때)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
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

