package com.watchserviceagent.watchservice_agent.settings;

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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/folders/browse")
    public ApiResponse<Map<String, Object>> browseDirectory(
            @RequestParam(value = "path", defaultValue = "") String path) {
        File dir;
        if (path == null || path.isBlank()) {
            dir = new File(System.getProperty("user.home"));
        } else {
            dir = new File(path);
        }

        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File(System.getProperty("user.home"));
        }

        File[] children = dir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
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

        String parentPath = dir.getParent();

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
}
