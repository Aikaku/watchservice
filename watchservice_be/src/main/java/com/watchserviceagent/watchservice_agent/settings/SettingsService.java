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
 * 클래스 이름 : SettingsService
 * 기능 : 감시 폴더·예외 규칙·감시 스케줄·이메일 알림 설정 비즈니스 로직.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingsRepository settingsRepository;

    // ===== 감시 폴더 =====

    /*
     * 함수 이름 : getWatchedFolders
     * 기능 : 지정한 ownerKey의 감시 폴더 목록을 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : List<WatchedFolderResponse>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<WatchedFolderResponse> getWatchedFolders(String ownerKey) {
        List<WatchedFolder> list = settingsRepository.findWatchedFolders(ownerKey);
        return list.stream()
                .map(WatchedFolderResponse::from)
                .collect(Collectors.toList());
    }

    /*
     * 함수 이름 : addWatchedFolder
     * 기능 : 감시 폴더를 추가한다. 경로 유효성을 검증하고 DB에 저장한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, WatchedFolderRequest req - 폴더 이름·경로
     * 반환값 : WatchedFolderResponse - 추가된 폴더 정보
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
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

    /*
     * 함수 이름 : deleteWatchedFolder
     * 기능 : 지정한 ID의 감시 폴더를 삭제한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, Long id - 폴더 PK
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void deleteWatchedFolder(String ownerKey, Long id) {
        settingsRepository.deleteWatchedFolder(ownerKey, id);
        log.info("[SettingsService] 감시 폴더 삭제: id={}", id);
    }

    // ===== 예외 규칙 =====

    /*
     * 함수 이름 : getExceptionRules
     * 기능 : 지정한 ownerKey의 예외 규칙 목록을 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : List<ExceptionRuleResponse>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<ExceptionRuleResponse> getExceptionRules(String ownerKey) {
        List<ExceptionRule> list = settingsRepository.findExceptionRules(ownerKey);
        return list.stream()
                .map(ExceptionRuleResponse::from)
                .collect(Collectors.toList());
    }

    /*
     * 함수 이름 : addExceptionRule
     * 기능 : 예외 규칙을 추가한다. type이 null이면 PATH로 기본 설정한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, ExceptionRuleRequest req - 타입·패턴·메모
     * 반환값 : ExceptionRuleResponse - 추가된 규칙 정보
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
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

    /*
     * 함수 이름 : deleteExceptionRule
     * 기능 : 지정한 ID의 예외 규칙을 삭제한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, Long id - 예외 규칙 PK
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void deleteExceptionRule(String ownerKey, Long id) {
        settingsRepository.deleteExceptionRule(ownerKey, id);
        log.info("[SettingsService] 예외 규칙 삭제: id={}", id);
    }

    // ===== 감시 스케줄 =====

    private static final String KEY_WATCH_SCHEDULE = "watch_schedule";

    /*
     * 함수 이름 : getWatchSchedule
     * 기능 : 감시 스케줄 설정 JSON을 반환한다. 미설정이면 기본값(평일 09:00~18:00 비활성)을 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : String - 스케줄 JSON
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public String getWatchSchedule(String ownerKey) {
        String v = settingsRepository.getAppSetting(ownerKey, KEY_WATCH_SCHEDULE);
        return v == null ? "{\"enabled\":false,\"days\":[1,2,3,4,5],\"startTime\":\"09:00\",\"endTime\":\"18:00\"}" : v;
    }

    /*
     * 함수 이름 : updateWatchSchedule
     * 기능 : 감시 스케줄 설정 JSON을 저장한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String scheduleJson - 스케줄 JSON
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void updateWatchSchedule(String ownerKey, String scheduleJson) {
        settingsRepository.setAppSetting(ownerKey, KEY_WATCH_SCHEDULE, scheduleJson);
        log.info("[SettingsService] 감시 스케줄 업데이트: ownerKey={}", ownerKey);
    }

    // ===== 이메일 알림 =====

    private static final String KEY_ALERT_EMAIL = "alert_email";

    /*
     * 함수 이름 : getAlertEmail
     * 기능 : 알림 수신 이메일 주소를 반환한다. 미설정이면 빈 문자열을 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : String - 이메일 주소
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public String getAlertEmail(String ownerKey) {
        String v = settingsRepository.getAppSetting(ownerKey, KEY_ALERT_EMAIL);
        return v == null ? "" : v;
    }

    /*
     * 함수 이름 : updateAlertEmail
     * 기능 : 알림 수신 이메일 주소를 저장한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String email - 이메일 주소 (null이면 빈 문자열로 처리)
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void updateAlertEmail(String ownerKey, String email) {
        if (email == null) email = "";
        settingsRepository.setAppSetting(ownerKey, KEY_ALERT_EMAIL, email.trim());
        log.info("[SettingsService] 알림 이메일 업데이트: ownerKey={}", ownerKey);
    }
}
