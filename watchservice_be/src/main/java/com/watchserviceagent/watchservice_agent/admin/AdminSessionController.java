package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.alerts.NotificationRepository;
import com.watchserviceagent.watchservice_agent.settings.SettingsRepository;
import com.watchserviceagent.watchservice_agent.storage.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 클래스 이름 : AdminSessionController
 * 기능 : 관리자 전용 세션(owner_key) 현황 API. 등록된 에이전트별 통계를 제공한다.
 * 경로 : /api/admin/sessions  (AdminAuthInterceptor에 의해 보호됨)
 */
@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final LogRepository logRepository;
    private final NotificationRepository notificationRepository;
    private final SettingsRepository settingsRepository;

    /*
     * 함수 이름 : getSessions
     * 기능 : 등록된 모든 에이전트(owner_key)별 로그, 알림, 감시 폴더, 예외 규칙 수를 조회한다.
     * 매개변수 : 없음
     * 반환값 : List<Map<String, Object>> - owner_key별 통계 목록 (로그 수 내림차순 정렬)
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping
    public List<Map<String, Object>> getSessions() {
        // 각 테이블에서 owner_key별 집계 수집
        Map<String, Long> logCounts = toMap(logRepository.countLogsByOwnerKey());
        Map<String, Long> alertCounts = toMap(notificationRepository.countNotificationsByOwnerKey());
        Map<String, Long> folderCounts = toMap(settingsRepository.countFoldersByOwnerKey());
        Map<String, Long> exceptionCounts = toMap(settingsRepository.countExceptionsByOwnerKey());

        // 모든 owner_key 수집 (테이블마다 다를 수 있음)
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(logCounts.keySet());
        allKeys.addAll(alertCounts.keySet());
        allKeys.addAll(folderCounts.keySet());
        allKeys.addAll(exceptionCounts.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : allKeys) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ownerKey", key);
            row.put("logCount", logCounts.getOrDefault(key, 0L));
            row.put("alertCount", alertCounts.getOrDefault(key, 0L));
            row.put("folderCount", folderCounts.getOrDefault(key, 0L));
            row.put("exceptionCount", exceptionCounts.getOrDefault(key, 0L));
            result.add(row);
        }

        // 로그 수 기준 내림차순 정렬
        result.sort((a, b) -> Long.compare((Long) b.get("logCount"), (Long) a.get("logCount")));
        return result;
    }

    /*
     * 함수 이름 : toMap
     * 기능 : 다형성 통계 객체 목록을 ownerKey → count 맵으로 변환한다.
     * 매개변수 : stats - OwnerKeyStat 구현 목록
     * 반환값 : Map<String, Long> - ownerKey별 카운트 맵
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private Map<String, Long> toMap(List<? extends Object> stats) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object stat : stats) {
            if (stat instanceof LogRepository.OwnerKeyStat s) {
                map.put(s.ownerKey(), s.count());
            } else if (stat instanceof NotificationRepository.OwnerKeyStat s) {
                map.put(s.ownerKey(), s.count());
            } else if (stat instanceof SettingsRepository.OwnerKeyStat s) {
                map.put(s.ownerKey(), s.count());
            }
        }
        return map;
    }
}
