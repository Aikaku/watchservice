package com.watchserviceagent.watchservice_agent.audit;

import com.watchserviceagent.watchservice_agent.settings.SettingsRepository;
import com.watchserviceagent.watchservice_agent.settings.domain.WatchedFolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 클래스 이름 : FilePermissionAuditService
 * 기능 : 감시 폴더 내 파일의 권한을 주기적으로 검사하여 others 쓰기·실행 권한이 있는 파일을 탐지한다.
 *        macOS/Linux: Files.getPosixFilePermissions() 사용.
 *        Windows: POSIX 미지원이므로 탐지를 건너뛰고 감사 불가 상태를 기록한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilePermissionAuditService {

    private final SettingsRepository settingsRepository;
    private final AuditRepository auditRepository;

    /** ownerKey → 마지막 감사 완료 시각 */
    private final Map<String, Instant> lastAuditTime = new ConcurrentHashMap<>();

    private static final boolean IS_POSIX =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    /**
     * 함수 이름 : runScheduledAudit
     * 기능 : 매 6시간마다 모든 사용자의 감시 폴더에 대해 파일 권한 감사를 수행한다.
     * 작성 날짜 : 2026/04/24
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L)
    public void runScheduledAudit() {
        List<SettingsRepository.OwnerKeyStat> ownerStats = settingsRepository.countFoldersByOwnerKey();
        for (SettingsRepository.OwnerKeyStat stat : ownerStats) {
            runAuditForOwner(stat.ownerKey());
        }
    }

    /**
     * 함수 이름 : runAuditForOwner
     * 기능 : 특정 사용자의 모든 감시 폴더를 즉시 감사한다. API를 통해 수동으로도 호출된다.
     * 매개변수 : ownerKey - 사용자 키
     * 반환값 : 발견된 위험 파일 수
     */
    public int runAuditForOwner(String ownerKey) {
        List<WatchedFolder> folders = settingsRepository.findWatchedFolders(ownerKey);
        if (folders.isEmpty()) return 0;

        List<AuditResult> results = new ArrayList<>();

        if (!IS_POSIX) {
            // Windows 환경: POSIX 미지원 알림 기록
            AuditResult res = AuditResult.builder()
                    .ownerKey(ownerKey)
                    .filePath("(Windows POSIX 미지원)")
                    .permissions("N/A")
                    .issue("이 OS는 POSIX 권한 감사를 지원하지 않습니다.")
                    .auditedAt(Instant.now())
                    .build();
            results.add(res);
        } else {
            for (WatchedFolder folder : folders) {
                try {
                    scanDirectory(ownerKey, Paths.get(folder.getPath()), results);
                } catch (Exception e) {
                    log.warn("[AuditService] 폴더 감사 실패: path={}", folder.getPath(), e);
                }
            }
        }

        if (!results.isEmpty()) {
            auditRepository.saveAll(results);
            log.info("[AuditService] 감사 완료: ownerKey={}, 위험파일={}건", ownerKey, results.size());
        }

        lastAuditTime.put(ownerKey, Instant.now());
        return results.size();
    }

    private void scanDirectory(String ownerKey, Path dir, List<AuditResult> results) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;

        Files.walkFileTree(dir, new HashSet<>(), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
                    boolean othersWrite   = perms.contains(PosixFilePermission.OTHERS_WRITE);
                    boolean othersExecute = perms.contains(PosixFilePermission.OTHERS_EXECUTE);

                    if (othersWrite || othersExecute) {
                        List<String> issues = new ArrayList<>();
                        if (othersWrite)   issues.add("others-write");
                        if (othersExecute) issues.add("others-execute");

                        AuditResult res = AuditResult.builder()
                                .ownerKey(ownerKey)
                                .filePath(file.toAbsolutePath().toString())
                                .permissions(PosixFilePermissions.toString(perms))
                                .issue(String.join(", ", issues))
                                .auditedAt(Instant.now())
                                .build();
                        results.add(res);
                    }
                } catch (Exception e) {
                    log.debug("[AuditService] 파일 권한 조회 실패: {}", file, e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Instant getLastAuditTime(String ownerKey) {
        return lastAuditTime.get(ownerKey);
    }
}
