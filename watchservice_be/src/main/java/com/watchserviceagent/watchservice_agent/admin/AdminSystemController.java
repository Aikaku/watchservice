package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.alerts.NotificationRepository;
import com.watchserviceagent.watchservice_agent.storage.LogRepository;
import com.watchserviceagent.watchservice_agent.watcher.WatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 클래스 이름 : AdminSystemController
 * 기능 : 관리자 전용 시스템 상태 API. DB 크기, 레코드 수, AI 서버 상태, Watcher 상태를 제공한다.
 * 경로 : /api/admin/system  (AdminAuthInterceptor에 의해 보호됨)
 */
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Slf4j
public class AdminSystemController {

    private final LogRepository logRepository;
    private final NotificationRepository notificationRepository;
    private final WatcherService watcherService;

    @Value("${ai.analyze.url:http://localhost:8001/api/analyze}")
    private String aiAnalyzeUrl;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /*
     * 함수 이름 : getSystemStatus
     * 기능 : DB 크기, 레코드 수, Watcher 상태, AI 서버 연결 상태를 포함한 시스템 현황을 조회한다.
     * 매개변수 : 없음
     * 반환값 : Map<String, Object> - 시스템 상태 정보
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new LinkedHashMap<>();

        // DB 파일 크기
        File dbFile = new File("log.db");
        result.put("dbSizeBytes", dbFile.exists() ? dbFile.length() : 0L);
        result.put("dbSizeMb", dbFile.exists() ? String.format("%.2f", dbFile.length() / 1024.0 / 1024.0) : "0.00");

        // 레코드 수
        result.put("totalLogs", logRepository.countTotalLogs());
        result.put("totalAlerts", notificationRepository.countTotalNotifications());
        result.put("totalSessions", logRepository.countLogsByOwnerKey().size());

        // Watcher 상태
        result.put("watcherRunning", watcherService.isAnyRunning());

        // AI 서버 상태
        result.put("aiServerStatus", checkAiServer());

        return result;
    }

    /*
     * 함수 이름 : checkAiServer
     * 기능 : AI 서버에 HTTP GET 요청을 보내 연결 가능 여부를 확인한다. 타임아웃은 2초.
     * 매개변수 : 없음
     * 반환값 : "UP" (정상), "DOWN" (연결 실패)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String checkAiServer() {
        try {
            // AI 서버의 base URL 추출 (http://localhost:8001)
            URI analyzeUri = URI.create(aiAnalyzeUrl);
            String base = analyzeUri.getScheme() + "://" + analyzeUri.getHost()
                    + (analyzeUri.getPort() > 0 ? ":" + analyzeUri.getPort() : "");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
            return "UP";
        } catch (Exception e) {
            log.debug("[AdminSystemController] AI 서버 연결 실패: {}", e.getMessage());
            return "DOWN";
        }
    }
}
