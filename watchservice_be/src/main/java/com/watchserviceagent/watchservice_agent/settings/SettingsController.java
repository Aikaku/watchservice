package com.watchserviceagent.watchservice_agent.settings;

import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.ExceptionRuleResponse;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderRequest;
import com.watchserviceagent.watchservice_agent.settings.dto.WatchedFolderResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 클래스 이름 : SettingsController
 * 기능 : 감시 폴더 및 예외 규칙 설정을 관리하는 REST API 엔드포인트를 제공한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 함수 이름 : browseDirectory
     * 기능 : 서버 파일시스템의 특정 경로에 있는 하위 디렉토리 목록을 반환한다.
     * 매개변수 : path - 탐색할 경로 (빈 값이면 홈 디렉토리)
     * 반환값 : 현재 경로와 하위 디렉토리 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    @GetMapping("/folders/browse")
    public Map<String, Object> browseDirectory(@RequestParam(value = "path", defaultValue = "") String path) {
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

        // 상위 폴더 경로 계산
        String parentPath = dir.getParent();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", dir.getAbsolutePath());
        result.put("parent", parentPath);
        result.put("entries", entries);
        log.info("[SettingsController] GET /settings/folders/browse?path={} -> {}개 항목", dir.getAbsolutePath(), entries.size());
        return result;
    }

    /**
     * 함수 이름 : getWatchedFolders
     * 기능 : 등록된 감시 폴더 목록을 조회한다.
     * 매개변수 : 없음
     * 반환값 : WatchedFolderResponse 리스트
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @GetMapping("/folders")
    public List<WatchedFolderResponse> getWatchedFolders(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<WatchedFolderResponse> list = settingsService.getWatchedFolders(ownerKey);
        log.info("[SettingsController] GET /settings/folders -> {}건", list.size());
        return list;
    }

    /**
     * 함수 이름 : addWatchedFolder
     * 기능 : 새로운 감시 폴더를 추가한다.
     * 매개변수 : req - 감시 폴더 요청 객체
     * 반환값 : WatchedFolderResponse - 추가된 감시 폴더 정보
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @PostMapping("/folders")
    public WatchedFolderResponse addWatchedFolder(@RequestBody WatchedFolderRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        WatchedFolderResponse resp = settingsService.addWatchedFolder(ownerKey, req);
        log.info("[SettingsController] POST /settings/folders -> {}", resp);
        return resp;
    }

    /**
     * 함수 이름 : deleteWatchedFolder
     * 기능 : 감시 폴더를 삭제한다.
     * 매개변수 : id - 삭제할 감시 폴더 ID
     * 반환값 : 없음
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @DeleteMapping("/folders/{id}")
    public void deleteWatchedFolder(@PathVariable("id") Long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        settingsService.deleteWatchedFolder(ownerKey, id);
        log.info("[SettingsController] DELETE /settings/folders/{}", id);
    }

    /**
     * 함수 이름 : getExceptionRules
     * 기능 : 등록된 예외 규칙 목록을 조회한다.
     * 매개변수 : 없음
     * 반환값 : ExceptionRuleResponse 리스트
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @GetMapping("/exceptions")
    public List<ExceptionRuleResponse> getExceptionRules(HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        List<ExceptionRuleResponse> list = settingsService.getExceptionRules(ownerKey);
        log.info("[SettingsController] GET /settings/exceptions -> {}건", list.size());
        return list;
    }

    /**
     * 함수 이름 : addExceptionRule
     * 기능 : 새로운 예외 규칙을 추가한다.
     * 매개변수 : req - 예외 규칙 요청 객체
     * 반환값 : ExceptionRuleResponse - 추가된 예외 규칙 정보
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @PostMapping("/exceptions")
    public ExceptionRuleResponse addExceptionRule(@RequestBody ExceptionRuleRequest req, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        ExceptionRuleResponse resp = settingsService.addExceptionRule(ownerKey, req);
        log.info("[SettingsController] POST /settings/exceptions -> {}", resp);
        return resp;
    }

    /**
     * 함수 이름 : deleteExceptionRule
     * 기능 : 예외 규칙을 삭제한다.
     * 매개변수 : id - 삭제할 예외 규칙 ID
     * 반환값 : 없음
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    @DeleteMapping("/exceptions/{id}")
    public void deleteExceptionRule(@PathVariable("id") Long id, HttpSession session) {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);
        settingsService.deleteExceptionRule(ownerKey, id);
        log.info("[SettingsController] DELETE /settings/exceptions/{}", id);
    }
}
