package com.watchserviceagent.watchservice_agent.settings;

import com.watchserviceagent.watchservice_agent.alerts.EmailNotificationService;
import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleResponse;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final SettingsService settingsService;
    private final EmailNotificationService emailNotificationService;

    @GetMapping("/folders/browse")
    public ApiResponse<Map<String, Object>> browseDirectory(
            @RequestParam(value = "path", defaultValue = "") String path) {

        boolean isWindows = File.separatorChar == '\\';

        // Windows: path="" -> 드라이브 목록 반환
        if (isWindows && (path == null || path.isBlank())) {
            File[] roots = File.listRoots();
            List<Map<String, String>> driveEntries = new ArrayList<>();
            if (roots != null) {
                for (File root : roots) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("name", root.getAbsolutePath());
                    entry.put("path", root.getAbsolutePath());
                    driveEntries.add(entry);
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("current", "");
            result.put("parent", null);
            result.put("entries", driveEntries);
            log.info("[SettingsController] GET /settings/folders/browse (Windows drive list) -> {}개 드라이브", driveEntries.size());
            return ApiResponse.ok(result);
        }

        File dir;
        if (path == null || path.isBlank()) {
            dir = new File(System.getProperty("user.home"));
        } else {
            dir = new File(path);
        }

        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File(System.getProperty("user.home"));
        }

        File[] children = dir.listFiles(f -> f.isDirectory() && !f.isHidden() && !f.getName().startsWith("."));
        List<Map<String, String>> entries = new ArrayList<>();
        if (children != null) {
            Arrays.stream(children)
                    .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(f -> {
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("name", f.getName());
                        entry.put("path", f.getAbsolutePath());
                        entries.add(entry);
                    });
        }

        // Windows 드라이브 루트(C:\)에서 getParent()는 null -> "" 으로 변환해 드라이브 목록으로 복귀
        String parentPath = dir.getParent();
        if (parentPath == null && isWindows) {
            parentPath = "";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", dir.getAbsolutePath());
        result.put("parent", parentPath);
        result.put("entries", entries);
        log.info("[SettingsController] GET /settings/folders/browse?path={} -> {}개 항목", dir.getAbsolutePath(), entries.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/folders")
    public ApiResponse<List<WatchedFolderResponse>> getWatchedFolders(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<WatchedFolderResponse> list = settingsService.getWatchedFolders(ownerKey);
        log.info("[SettingsController] GET /settings/folders -> {}건", list.size());
        return ApiResponse.ok(list);
    }

    @PostMapping("/folders")
    public ApiResponse<WatchedFolderResponse> addWatchedFolder(
            @Valid @RequestBody WatchedFolderRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        WatchedFolderResponse resp = settingsService.addWatchedFolder(ownerKey, req);
        log.info("[SettingsController] POST /settings/folders -> {}", resp);
        return ApiResponse.ok(resp);
    }

    @DeleteMapping("/folders/{id}")
    public ApiResponse<Void> deleteWatchedFolder(@PathVariable("id") Long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        settingsService.deleteWatchedFolder(ownerKey, id);
        log.info("[SettingsController] DELETE /settings/folders/{}", id);
        return ApiResponse.ok();
    }

    @GetMapping("/exceptions")
    public ApiResponse<List<ExceptionRuleResponse>> getExceptionRules(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<ExceptionRuleResponse> list = settingsService.getExceptionRules(ownerKey);
        log.info("[SettingsController] GET /settings/exceptions -> {}건", list.size());
        return ApiResponse.ok(list);
    }

    @PostMapping("/exceptions")
    public ApiResponse<ExceptionRuleResponse> addExceptionRule(
            @Valid @RequestBody ExceptionRuleRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        ExceptionRuleResponse resp = settingsService.addExceptionRule(ownerKey, req);
        log.info("[SettingsController] POST /settings/exceptions -> {}", resp);
        return ApiResponse.ok(resp);
    }

    @DeleteMapping("/exceptions/{id}")
    public ApiResponse<Void> deleteExceptionRule(@PathVariable("id") Long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        settingsService.deleteExceptionRule(ownerKey, id);
        log.info("[SettingsController] DELETE /settings/exceptions/{}", id);
        return ApiResponse.ok();
    }

    // ===== 이메일 알림 설정 =====

    @GetMapping("/alert-email")
    public ApiResponse<Map<String, String>> getAlertEmail(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        String email = settingsService.getAlertEmail(ownerKey);
        log.info("[SettingsController] GET /settings/alert-email ownerKey={}", ownerKey);
        return ApiResponse.ok(Map.of("email", email));
    }

    @PutMapping("/alert-email")
    public ApiResponse<Void> updateAlertEmail(@RequestBody Map<String, String> body, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        String email = body.getOrDefault("email", "");
        settingsService.updateAlertEmail(ownerKey, email);
        log.info("[SettingsController] PUT /settings/alert-email ownerKey={}", ownerKey);
        return ApiResponse.ok();
    }

    // ===== 감시 스케줄 =====

    @GetMapping("/schedule")
    public ApiResponse<Map<String, Object>> getWatchSchedule(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        String json = settingsService.getWatchSchedule(ownerKey);
        log.info("[SettingsController] GET /settings/schedule ownerKey={}", ownerKey);
        return ApiResponse.ok(Map.of("schedule", json));
    }

    @PutMapping("/schedule")
    public ApiResponse<Void> updateWatchSchedule(
            @RequestBody Map<String, String> body, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        String json = body.getOrDefault("schedule", "{}");
        settingsService.updateWatchSchedule(ownerKey, json);
        log.info("[SettingsController] PUT /settings/schedule ownerKey={}", ownerKey);
        return ApiResponse.ok();
    }

    @PostMapping("/alert-email/test")
    public ApiResponse<Void> sendTestEmail(@RequestBody Map<String, String> body, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        String email = body.getOrDefault("email", "");
        if (email.isBlank()) {
            throw new IllegalArgumentException("이메일 주소를 입력해 주세요.");
        }
        emailNotificationService.sendTestEmail(email);
        log.info("[SettingsController] POST /settings/alert-email/test ownerKey={}", ownerKey);
        return ApiResponse.ok();
    }
}
