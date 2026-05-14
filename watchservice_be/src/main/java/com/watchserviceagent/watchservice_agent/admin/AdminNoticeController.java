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

    /*
     * 함수 이름 : getNotices
     * 기능 : 사용자 및 관리자 공용 공지사항 목록을 조회한다.
     * 매개변수 : 없음
     * 반환값 : List<NoticeDto> - 공지사항 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/api/notifications")
    public List<NoticeDto> getNotices() {
        return noticeService.getAll();
    }

    /*
     * 함수 이름 : createNotice
     * 기능 : 관리자 권한으로 공지사항을 등록한다.
     * 매개변수 : req - 등록할 공지사항 요청 (title, content)
     * 반환값 : 200 OK (성공), 400 Bad Request (content 누락)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @PostMapping("/api/admin/notifications")
    public ResponseEntity<?> createNotice(@RequestBody NoticeRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }
        noticeService.create(req.getTitle(), req.getContent());
        return ResponseEntity.ok().build();
    }

    /*
     * 함수 이름 : deleteNotice
     * 기능 : 관리자 권한으로 특정 ID의 공지사항을 삭제한다.
     * 매개변수 : id - 삭제할 공지사항 ID
     * 반환값 : 204 No Content (성공), 404 Not Found (없을 때)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
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

