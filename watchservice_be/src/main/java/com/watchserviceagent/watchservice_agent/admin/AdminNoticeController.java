package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.dto.NoticeDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클래스 이름 : AdminNoticeController
 * 기능 : 공지사항 조회 및 관리자 공지 CRUD API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    /**
     * 사용자/관리자 공용: 공지사항 목록 조회
     * GET /api/notifications
     */
    @GetMapping("/api/notifications")
    public List<NoticeDto> getNotices() {
        return noticeService.getAll();
    }

    /**
     * 관리자: 공지사항 등록
     * POST /api/admin/notifications
     */
    @PostMapping("/api/admin/notifications")
    public ResponseEntity<?> createNotice(@RequestBody NoticeRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }
        noticeService.create(req.getTitle(), req.getContent());
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자: 공지사항 삭제
     * DELETE /api/admin/notifications/{id}
     */
    @DeleteMapping("/api/admin/notifications/{id}")
    public ResponseEntity<?> deleteNotice(@PathVariable long id) {
        int deleted = noticeService.deleteById(id);
        if (deleted <= 0) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Setter
    public static class NoticeRequest {
        private String title;
        private String content;
    }
}

