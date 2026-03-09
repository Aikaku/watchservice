package com.watchserviceagent.watchservice_agent.watcher;

import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 클래스 이름 : WatcherController
 * 기능 : 파일 감시 시작/중지 요청을 처리하는 REST API 엔드포인트를 제공한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
@Slf4j
@RestController
@RequestMapping("/watcher")
@RequiredArgsConstructor
public class WatcherController {

    private final WatcherService watcherService;

    @PostMapping("/start")
    public ResponseEntity<String> startWatchingPost(@RequestParam("folderPath") String folderPath,
                                                    HttpSession session) {
        return startInternal(OwnerKeyUtil.getOrCreate(session), folderPath, "POST");
    }

    @GetMapping("/start")
    public ResponseEntity<String> startWatchingGet(@RequestParam("folderPath") String folderPath,
                                                   HttpSession session) {
        return startInternal(OwnerKeyUtil.getOrCreate(session), folderPath, "GET");
    }

    private ResponseEntity<String> startInternal(String ownerKey, String folderPath, String method) {
        log.info("[WatcherController] 감시 시작 요청 (method={}) - folderPath={}", method, folderPath);
        try {
            watcherService.startWatching(ownerKey, folderPath);
            return ResponseEntity.ok("[Watcher] 감시를 시작했습니다: " + folderPath);
        } catch (Exception e) {
            log.error("[WatcherController] 감시 시작 실패", e);
            return ResponseEntity.internalServerError()
                    .body("[Watcher] 감시 시작 실패: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopWatchingPost(HttpSession session) {
        return stopInternal(OwnerKeyUtil.getOrCreate(session), "POST");
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stopWatchingGet(HttpSession session) {
        return stopInternal(OwnerKeyUtil.getOrCreate(session), "GET");
    }

    private ResponseEntity<String> stopInternal(String ownerKey, String method) {
        log.info("[WatcherController] 감시 중지 요청 (method={})", method);
        try {
            watcherService.stopWatching(ownerKey);
            return ResponseEntity.ok("[Watcher] 감시를 중지했습니다.");
        } catch (Exception e) {
            log.error("[WatcherController] 감시 중지 실패", e);
            return ResponseEntity.internalServerError()
                    .body("[Watcher] 감시 중지 실패: " + e.getMessage());
        }
    }
}
