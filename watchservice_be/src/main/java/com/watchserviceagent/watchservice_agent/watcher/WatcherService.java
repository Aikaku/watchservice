package com.watchserviceagent.watchservice_agent.watcher;

import com.watchserviceagent.watchservice_agent.analytics.EventWindowAggregator;
import com.watchserviceagent.watchservice_agent.collector.FileCollectorService;
import com.watchserviceagent.watchservice_agent.collector.dto.FileAnalysisResult;
import com.watchserviceagent.watchservice_agent.watcher.dto.WatcherEventRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클래스 이름 : WatcherService
 * 기능 : 지정된 폴더를 실시간으로 감시하여 파일 생성/수정/삭제 이벤트를 감지하고, 이벤트를 Collector로 전달한다.
 *        사용자별(ownerKey)로 독립적인 WatchService 인스턴스를 관리한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatcherService {

    private final FileCollectorService fileCollectorService;
    private final EventWindowAggregator eventWindowAggregator;

    // 사용자별 감시 상태
    private static class UserWatcherState {
        final String ownerKey;
        WatchService watchService;
        Thread watcherThread;
        volatile boolean running = false;
        final Map<WatchKey, Path> keyDirMap = new ConcurrentHashMap<>();
        final List<Path> watchedRoots = new ArrayList<>();

        UserWatcherState(String ownerKey) {
            this.ownerKey = ownerKey;
        }
    }

    private final Map<String, UserWatcherState> userWatcherStates = new ConcurrentHashMap<>();

    private UserWatcherState getOrCreateState(String ownerKey) {
        return userWatcherStates.computeIfAbsent(ownerKey, UserWatcherState::new);
    }

    /**
     * 함수 이름 : startWatching
     * 기능 : 단일 폴더 경로를 감시하기 시작한다.
     * 매개변수 : ownerKey - 사용자 키, folderPath - 감시할 폴더 경로
     * 반환값 : 없음
     * 예외 : IOException - WatchService 생성 실패 시
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public void startWatching(String ownerKey, String folderPath) throws IOException {
        startWatchingMultiple(ownerKey, List.of(folderPath));
    }

    /**
     * 함수 이름 : startWatchingMultiple
     * 기능 : 여러 폴더 경로를 동시에 감시하기 시작한다. WatchService를 초기화하고 모든 하위 디렉토리를 재귀적으로 등록한다.
     * 매개변수 : ownerKey - 사용자 키, folderPaths - 감시할 폴더 경로 리스트
     * 반환값 : 없음
     * 예외 : IOException - WatchService 생성 실패 시, IllegalArgumentException - 경로가 유효하지 않을 때
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public synchronized void startWatchingMultiple(String ownerKey, List<String> folderPaths) throws IOException {
        UserWatcherState state = getOrCreateState(ownerKey);

        if (state.running) {
            log.info("Watcher already running for ownerKey={}. paths={}", ownerKey, folderPaths);
            return;
        }

        List<Path> roots = new ArrayList<>();
        for (String p : (folderPaths == null ? List.<String>of() : folderPaths)) {
            if (p == null || p.isBlank()) continue;
            Path root = Paths.get(p.trim());
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                throw new IllegalArgumentException("감시할 경로가 존재하지 않거나 디렉토리가 아닙니다: " + p);
            }
            roots.add(root);
        }

        if (roots.isEmpty()) {
            throw new IllegalArgumentException("감시할 폴더가 없습니다.");
        }

        state.watchService = FileSystems.getDefault().newWatchService();
        state.keyDirMap.clear();
        state.watchedRoots.clear();
        state.watchedRoots.addAll(roots);

        for (Path root : roots) {
            registerAll(state, root);
        }

        state.running = true;
        state.watcherThread = new Thread(() -> watchLoop(state), "WatcherService-Thread-" + ownerKey);
        state.watcherThread.start();

        log.info("Started watching roots={} for ownerKey={}", roots, ownerKey);
    }

    private void registerAll(UserWatcherState state, Path start) throws IOException {
        Files.walkFileTree(start, new HashSet<>(), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(
                        state.watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                );
                state.keyDirMap.put(key, dir);
                log.debug("Registered directory for watching: {}", dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 함수 이름 : stopWatching
     * 기능 : 해당 사용자의 파일 감시를 중지하고 리소스를 정리한다. 남은 윈도우 이벤트를 flush한다.
     * 매개변수 : ownerKey - 사용자 키
     * 반환값 : 없음
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public synchronized void stopWatching(String ownerKey) {
        UserWatcherState state = userWatcherStates.get(ownerKey);
        if (state == null || !state.running) {
            log.info("Watcher is not running for ownerKey={}. ignore stopWatching.", ownerKey);
            return;
        }

        state.running = false;

        if (state.watchService != null) {
            try {
                state.watchService.close();
            } catch (IOException e) {
                log.warn("Failed to close WatchService for ownerKey={}", ownerKey, e);
            }
        }

        if (state.watcherThread != null) {
            state.watcherThread.interrupt();
            try {
                state.watcherThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for watcherThread to join", e);
            }
        }

        state.keyDirMap.clear();
        state.watchedRoots.clear();
        userWatcherStates.remove(ownerKey);

        // 종료 시 남은 윈도우 flush
        eventWindowAggregator.flushIfNeeded(ownerKey);

        log.info("Stopped watching for ownerKey={}", ownerKey);
    }

    /**
     * 함수 이름 : watchLoop
     * 기능 : WatchService에서 이벤트를 지속적으로 수신하여 처리하는 메인 루프.
     * 매개변수 : state - 사용자 감시 상태
     * 반환값 : 없음
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    private void watchLoop(UserWatcherState state) {
        String ownerKey = state.ownerKey;
        log.info("Watcher loop started. ownerKey={}", ownerKey);

        try {
            while (state.running) {
                WatchKey key;
                try {
                    key = state.watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Watcher loop interrupted");
                    break;
                } catch (ClosedWatchServiceException e) {
                    log.info("WatchService has been closed. stop watcher loop.");
                    break;
                }

                Path dir = state.keyDirMap.get(key);
                if (dir == null) {
                    boolean valid = key.reset();
                    if (!valid) state.keyDirMap.remove(key);
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("WatchService overflow event occurred. some events may have been lost.");
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name);

                    String eventType = mapKindToEventType(kind);

                    WatcherEventRecord record = WatcherEventRecord.builder()
                            .ownerKey(ownerKey)
                            .eventType(eventType)
                            .path(child.toAbsolutePath().toString())
                            .eventTimeMs(System.currentTimeMillis())
                            .build();

                    FileAnalysisResult analysisResult = fileCollectorService.analyze(record);

                    // ✅ 윈도우 집계+AI+로그 저장
                    eventWindowAggregator.onFileAnalysisResult(analysisResult);

                    // 새 폴더 생성되면 자동 등록
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child)) {
                            try {
                                registerAll(state, child);
                            } catch (IOException e) {
                                log.warn("Failed to register sub directory: {}", child, e);
                            }
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    state.keyDirMap.remove(key);
                    if (state.keyDirMap.isEmpty()) {
                        log.info("No directories are being watched anymore. stopping watchLoop.");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in watcher loop", e);
        } finally {
            state.running = false;
            log.info("Watcher loop finished for ownerKey={}", ownerKey);
        }
    }

    /**
     * 함수 이름 : mapKindToEventType
     * 기능 : WatchService의 이벤트 종류를 내부 이벤트 타입 문자열로 변환한다.
     * 매개변수 : kind - WatchService 이벤트 종류
     * 반환값 : "CREATE", "MODIFY", "DELETE", 또는 "UNKNOWN"
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    private String mapKindToEventType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) return "CREATE";
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) return "MODIFY";
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) return "DELETE";
        return "UNKNOWN";
    }

    /**
     * 함수 이름 : isRunning
     * 기능 : 해당 사용자의 파일 감시가 실행 중인지 여부를 반환한다.
     * 매개변수 : ownerKey - 사용자 키
     * 반환값 : true면 실행 중, false면 중지됨
     * 작성 날짜 : 2025/12/17
     * 작성자 : 시스템
     */
    public boolean isRunning(String ownerKey) {
        UserWatcherState state = userWatcherStates.get(ownerKey);
        return state != null && state.running;
    }

    public boolean isAnyRunning() {
        return userWatcherStates.values().stream().anyMatch(s -> s.running);
    }
}
