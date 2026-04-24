package com.watchserviceagent.watchservice_agent.watcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchserviceagent.watchservice_agent.settings.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * 클래스 이름 : WatchSchedulerService
 * 기능 : 매 1분마다 사용자별 감시 스케줄을 확인하여 감시를 자동으로 시작·중단한다.
 *        스케줄은 app_settings 테이블의 watch_schedule 키에 JSON으로 저장된다.
 *        형식: {"enabled":true,"days":[1,2,3,4,5],"startTime":"09:00","endTime":"18:00"}
 *        days: 1=월요일 … 7=일요일 (ISO-8601)
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchSchedulerService {

    private final WatcherService watcherService;
    private final SettingsRepository settingsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 함수 이름 : checkSchedule
     * 기능 : 매 1분마다 각 사용자의 감시 스케줄을 확인하여 감시를 자동으로 시작·중단한다.
     * 작성 날짜 : 2026/04/24
     */
    @Scheduled(fixedRate = 60_000)
    public void checkSchedule() {
        // 감시 폴더가 등록된 모든 ownerKey 목록 조회
        List<SettingsRepository.OwnerKeyStat> ownerStats = settingsRepository.countFoldersByOwnerKey();

        for (SettingsRepository.OwnerKeyStat stat : ownerStats) {
            String ownerKey = stat.ownerKey();
            try {
                String json = settingsRepository.getAppSetting(ownerKey, "watch_schedule");
                if (json == null || json.isBlank()) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> schedule = objectMapper.readValue(json, Map.class);

                Boolean enabled = (Boolean) schedule.get("enabled");
                if (enabled == null || !enabled) continue;

                boolean shouldRun = shouldRunNow(schedule);
                boolean isRunning = watcherService.isRunning(ownerKey);

                if (shouldRun && !isRunning) {
                    log.info("[WatchScheduler] 스케줄: 감시 재개 ownerKey={}", ownerKey);
                    watcherService.resumeWatcher(ownerKey);
                } else if (!shouldRun && isRunning) {
                    log.info("[WatchScheduler] 스케줄: 감시 중단 ownerKey={}", ownerKey);
                    watcherService.pauseWatcher(ownerKey);
                }
            } catch (Exception e) {
                log.warn("[WatchScheduler] 스케줄 처리 실패: ownerKey={}", ownerKey, e);
            }
        }
    }

    /**
     * 함수 이름 : shouldRunNow
     * 기능 : 현재 시각이 스케줄 범위 안에 있는지 판단한다.
     * 매개변수 : schedule - 스케줄 설정 맵
     * 반환값 : true이면 감시 활성 시간대, false이면 비활성 시간대
     */
    private boolean shouldRunNow(Map<String, Object> schedule) {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            DayOfWeek todayIso = now.getDayOfWeek(); // 1=월 … 7=일

            @SuppressWarnings("unchecked")
            List<Integer> days = (List<Integer>) schedule.get("days");
            if (days == null || days.isEmpty()) return true;

            boolean dayMatch = days.stream().anyMatch(d -> d == todayIso.getValue());
            if (!dayMatch) return false;

            String startStr = (String) schedule.get("startTime");
            String endStr   = (String) schedule.get("endTime");
            if (startStr == null || endStr == null) return true;

            LocalTime start = LocalTime.parse(startStr);
            LocalTime end   = LocalTime.parse(endStr);
            LocalTime nowTime = now.toLocalTime();

            return !nowTime.isBefore(start) && nowTime.isBefore(end);
        } catch (Exception e) {
            log.warn("[WatchScheduler] shouldRunNow 파싱 실패", e);
            return true;
        }
    }
}
