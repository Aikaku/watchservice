package com.watchserviceagent.watchservice_agent.scan;

import com.watchserviceagent.watchservice_agent.collector.FileCollectorService;
import com.watchserviceagent.watchservice_agent.collector.dto.FileAnalysisResult;
import com.watchserviceagent.watchservice_agent.scan.domain.ScanJob;
import com.watchserviceagent.watchservice_agent.scan.dto.ScanProgressResponse;
import com.watchserviceagent.watchservice_agent.watcher.WatcherService;
import com.watchserviceagent.watchservice_agent.watcher.dto.WatcherEventRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 클래스 이름 : ScanService
 * 기능 : 즉시 검사(스캔) 작업을 생성·실행·조회·중단한다. 스캔 완료 후 WatcherService 자동 시작을 지원한다.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final FileCollectorService fileCollectorService;
    private final WatcherService watcherService;

    private final Map<String, ScanJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /*
     * 함수 이름 : startScan
     * 기능 : 스캔 작업을 생성하고 비동기로 실행한다. 스캔 ID를 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, List<String> paths - 스캔 루트 경로 목록, boolean autoStartWatcher - 완료 후 watcher 자동 시작 여부
     * 반환값 : String - 발급된 scanId
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public String startScan(String ownerKey, List<String> paths, boolean autoStartWatcher) {
        List<String> roots = (paths == null) ? List.of() : paths.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (roots.isEmpty()) {
            throw new IllegalArgumentException("scan paths is empty");
        }

        String scanId = UUID.randomUUID().toString();
        ScanJob job = new ScanJob(scanId, roots);
        jobs.put(scanId, job);

        executor.submit(() -> runScan(job, autoStartWatcher, ownerKey));

        return scanId;
    }

    /*
     * 함수 이름 : pause
     * 기능 : 진행 중인 스캔에 중단 신호를 보낸다.
     * 매개변수 : String scanId - 스캔 ID
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void pause(String scanId) {
        ScanJob job = getJobOrThrow(scanId);
        job.pause();
    }

    /*
     * 함수 이름 : getProgress
     * 기능 : 스캔 작업의 현재 진행 상태(퍼센트, 스캔 수, 경로 등)를 반환한다.
     * 매개변수 : String scanId - 스캔 ID
     * 반환값 : ScanProgressResponse
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public ScanProgressResponse getProgress(String scanId) {
        ScanJob job = getJobOrThrow(scanId);

        return ScanProgressResponse.builder()
                .status(job.getStatus().name())
                .percent(job.getPercent())
                .scanned(job.getScanned().get())
                .total(job.getTotal().get())
                .currentPath(job.getCurrentPath())
                .message(job.getMessage())
                .build();
    }

    /*
     * 함수 이름 : getJobOrThrow
     * 기능 : scanId로 ScanJob을 조회한다. 없으면 IllegalArgumentException을 던진다.
     * 매개변수 : String scanId - 스캔 ID
     * 반환값 : ScanJob
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private ScanJob getJobOrThrow(String scanId) {
        ScanJob job = jobs.get(scanId);
        if (job == null) throw new IllegalArgumentException("scan not found: " + scanId);
        return job;
    }

    /*
     * 함수 이름 : runScan
     * 기능 : 스캔 작업을 실제로 실행한다. 루트 경로를 재귀 탐색하며 FileCollectorService로 snapshot을 채우고, 완료 후 autoStartWatcher가 true이면 WatcherService를 시작한다.
     * 매개변수 : ScanJob job - 스캔 작업 객체, boolean autoStartWatcher - 완료 후 watcher 자동 시작 여부, String ownerKey - 세션 소유자 키
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private void runScan(ScanJob job, boolean autoStartWatcher, String ownerKey) {

        try {
            // 1) total 계산(진행률용)
            long total = countTotalFiles(job.getRoots());
            job.setTotal(total);
            log.info("[ScanService] scanId={} totalFiles={}", job.getScanId(), total);

            // 2) 실제 스캔 수행(기준값 등록 = snapshot baseline/last 채움)
            for (String rootStr : job.getRoots()) {
                if (job.isStopRequested()) break;

                Path root;
                try {
                    root = Paths.get(rootStr);
                } catch (Exception e) {
                    log.warn("[ScanService] invalid root path string: {}", rootStr);
                    continue;
                }

                if (!Files.exists(root) || !Files.isDirectory(root)) {
                    log.warn("[ScanService] skip invalid root: {}", rootStr);
                    continue;
                }

                try (var stream = Files.walk(root)) {
                    Iterator<Path> it = stream
                            .filter(Files::isRegularFile)
                            .iterator();

                    while (it.hasNext()) {
                        if (job.isStopRequested()) break;

                        Path path = it.next();
                        String p = path.toAbsolutePath().toString();
                        job.setCurrentPath(p);

                        // eventType=SCAN : Collector가 snapshot baseline/last 채우도록 함
                        WatcherEventRecord rec = WatcherEventRecord.builder()
                                .ownerKey(ownerKey)
                                .eventType("SCAN")
                                .path(p)
                                .eventTimeMs(System.currentTimeMillis())
                                .build();

                        // ✅ 중요한 점: SCAN은 “로그 저장/AI 집계” 하지 않는다 (폭발 방지)
                        // Collector 내부에서 snapshot만 채워짐
                        FileAnalysisResult ignored = fileCollectorService.analyze(rec);

                        job.incScanned();
                    }
                }
            }

            if (job.isStopRequested()) {
                log.info("[ScanService] scanId={} PAUSED scanned={}/{}", job.getScanId(), job.getScanned().get(), job.getTotal().get());
                return;
            }

            job.done();
            log.info("[ScanService] scanId={} DONE scanned={}/{}", job.getScanId(), job.getScanned().get(), job.getScanned().get());

            // 3) scan 완료 후 watcher 자동 시작 (유효 root만!)
            if (autoStartWatcher) {
                List<String> validRoots = job.getRoots().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .filter(s -> {
                            try {
                                Path p = Paths.get(s);
                                return Files.exists(p) && Files.isDirectory(p);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .toList();

                if (!validRoots.isEmpty()) {
                    try {
                        watcherService.startWatchingMultiple(ownerKey, validRoots);
                        log.info("[ScanService] scanId={} watcher auto-start OK roots={}", job.getScanId(), validRoots);
                    } catch (Exception e) {
                        log.warn("[ScanService] watcher auto-start failed: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("[ScanService] scanId={} watcher auto-start skipped (no valid roots)", job.getScanId());
                }
            }

        } catch (Exception e) {
            log.error("[ScanService] scanId={} ERROR", job.getScanId(), e);
            job.error(e.getMessage());
        } finally {
            job.setCurrentPath(null);
        }
    }

    /*
     * 함수 이름 : countTotalFiles
     * 기능 : 스캔 루트 경로 목록에서 총 파일 수를 계산한다. 진행률 퍼센트 산출에 사용된다.
     * 매개변수 : List<String> roots - 루트 경로 목록
     * 반환값 : long - 총 파일 수
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private long countTotalFiles(List<String> roots) {
        long total = 0;
        for (String rootStr : (roots == null ? List.<String>of() : roots)) {
            try {
                Path root = Paths.get(rootStr);
                if (!Files.exists(root) || !Files.isDirectory(root)) continue;

                try (var stream = Files.walk(root)) {
                    total += stream.filter(Files::isRegularFile).count();
                }
            } catch (IOException ignore) {
            } catch (Exception ignore) {
            }
        }
        return total;
    }
}
