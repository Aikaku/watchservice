package com.watchserviceagent.watchservice_agent.settings;

import com.watchserviceagent.watchservice_agent.settings.domain.WatchedFolder;
import com.watchserviceagent.watchservice_agent.settings.domain.ExceptionRule;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderResponse;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 설정(감시 폴더, 예외 규칙) 비즈니스 로직.
 *
 * - SessionIdManager 를 통해 ownerKey 를 자동으로 부여/필터링한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingsRepository settingsRepository;

    // ===== 감시 폴더 =====

    public List<WatchedFolderResponse> getWatchedFolders(String ownerKey) {
        List<WatchedFolder> list = settingsRepository.findWatchedFolders(ownerKey);
        return list.stream()
                .map(WatchedFolderResponse::from)
                .collect(Collectors.toList());
    }

    public WatchedFolderResponse addWatchedFolder(String ownerKey, WatchedFolderRequest req) {
        if (req.getPath() == null || req.getPath().isBlank()) {
            throw new IllegalArgumentException("경로를 입력해 주세요.");
        }
        if (!Files.isDirectory(Paths.get(req.getPath().trim()))) {
            throw new IllegalArgumentException("존재하지 않는 폴더 경로입니다: " + req.getPath());
        }

        String name = (req.getName() == null || req.getName().isBlank())
                ? req.getPath()
                : req.getName();

        WatchedFolder folder = settingsRepository.insertWatchedFolder(ownerKey, name, req.getPath().trim());
        log.info("[SettingsService] 감시 폴더 추가: {}", folder);
        return WatchedFolderResponse.from(folder);
    }

    public void deleteWatchedFolder(String ownerKey, Long id) {
        settingsRepository.deleteWatchedFolder(ownerKey, id);
        log.info("[SettingsService] 감시 폴더 삭제: id={}", id);
    }

    // ===== 예외 규칙 =====

    public List<ExceptionRuleResponse> getExceptionRules(String ownerKey) {
        List<ExceptionRule> list = settingsRepository.findExceptionRules(ownerKey);
        return list.stream()
                .map(ExceptionRuleResponse::from)
                .collect(Collectors.toList());
    }

    public ExceptionRuleResponse addExceptionRule(String ownerKey, ExceptionRuleRequest req) {
        String type = (req.getType() == null || req.getType().isBlank())
                ? "PATH"
                : req.getType().toUpperCase();

        ExceptionRule rule = settingsRepository.insertExceptionRule(
                ownerKey,
                type,
                req.getPattern(),
                req.getMemo()
        );
        log.info("[SettingsService] 예외 규칙 추가: {}", rule);
        return ExceptionRuleResponse.from(rule);
    }

    public void deleteExceptionRule(String ownerKey, Long id) {
        settingsRepository.deleteExceptionRule(ownerKey, id);
        log.info("[SettingsService] 예외 규칙 삭제: id={}", id);
    }

    // ===== 감시 스케줄 =====

    private static final String KEY_WATCH_SCHEDULE = "watch_schedule";

    public String getWatchSchedule(String ownerKey) {
        String v = settingsRepository.getAppSetting(ownerKey, KEY_WATCH_SCHEDULE);
        return v == null ? "{\"enabled\":false,\"days\":[1,2,3,4,5],\"startTime\":\"09:00\",\"endTime\":\"18:00\"}" : v;
    }

    public void updateWatchSchedule(String ownerKey, String scheduleJson) {
        settingsRepository.setAppSetting(ownerKey, KEY_WATCH_SCHEDULE, scheduleJson);
        log.info("[SettingsService] 감시 스케줄 업데이트: ownerKey={}", ownerKey);
    }

    // ===== 이메일 알림 =====

    private static final String KEY_ALERT_EMAIL = "alert_email";

    public String getAlertEmail(String ownerKey) {
        String v = settingsRepository.getAppSetting(ownerKey, KEY_ALERT_EMAIL);
        return v == null ? "" : v;
    }

    public void updateAlertEmail(String ownerKey, String email) {
        if (email == null) email = "";
        settingsRepository.setAppSetting(ownerKey, KEY_ALERT_EMAIL, email.trim());
        log.info("[SettingsService] 알림 이메일 업데이트: ownerKey={}", ownerKey);
    }
}
