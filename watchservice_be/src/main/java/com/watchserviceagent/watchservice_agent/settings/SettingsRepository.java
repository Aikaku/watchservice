package com.watchserviceagent.watchservice_agent.settings;

import com.watchserviceagent.watchservice_agent.settings.domain.WatchedFolder;
import com.watchserviceagent.watchservice_agent.settings.domain.ExceptionRule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * 클래스 이름 : SettingsRepository
 * 기능 : 감시 폴더·예외 규칙·앱 설정(key-value) SQLite 접근 레이어.
 * 작성 날짜 : 2026/03/04
 * 작성자 : 이상혁
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class SettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    /*
     * 함수 이름 : init
     * 기능 : 애플리케이션 시작 시 watched_folder·exception_rule·app_settings 테이블을 생성한다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    @PostConstruct
    public void init() {
        createWatchedFolderTable();
        createExceptionRuleTable();
        createAppSettingsTable();
    }

    /*
     * 함수 이름 : createWatchedFolderTable
     * 기능 : watched_folder 테이블을 생성한다 (없으면 생성).
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private void createWatchedFolderTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS watched_folder (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_key  TEXT NOT NULL,
                    name       TEXT NOT NULL,
                    path       TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
                """;
        jdbcTemplate.execute(sql);
        log.info("[SettingsRepository] watched_folder 테이블 초기화 완료");
    }

    /*
     * 함수 이름 : createAppSettingsTable
     * 기능 : app_settings 테이블을 생성한다 (없으면 생성).
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private void createAppSettingsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_settings (
                    owner_key  TEXT NOT NULL,
                    key        TEXT NOT NULL,
                    value      TEXT,
                    PRIMARY KEY (owner_key, key)
                );
                """;
        jdbcTemplate.execute(sql);
        log.info("[SettingsRepository] app_settings 테이블 초기화 완료");
    }

    // ===== app_settings (key-value) =====

    /*
     * 함수 이름 : getAppSetting
     * 기능 : app_settings 테이블에서 key에 해당하는 값을 조회한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String key - 설정 키
     * 반환값 : String - 설정 값 (없으면 null)
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public String getAppSetting(String ownerKey, String key) {
        String sql = "SELECT value FROM app_settings WHERE owner_key = ? AND key = ? LIMIT 1";
        List<String> rows = jdbcTemplate.query(sql, (rs, i) -> rs.getString("value"), ownerKey, key);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /*
     * 함수 이름 : setAppSetting
     * 기능 : app_settings 테이블에 key-value를 저장한다. 이미 존재하면 value를 갱신한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String key - 설정 키, String value - 설정 값
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void setAppSetting(String ownerKey, String key, String value) {
        String sql = """
                INSERT INTO app_settings (owner_key, key, value)
                VALUES (?, ?, ?)
                ON CONFLICT(owner_key, key) DO UPDATE SET value = excluded.value
                """;
        jdbcTemplate.update(sql, ownerKey, key, value);
    }

    /*
     * 함수 이름 : createExceptionRuleTable
     * 기능 : exception_rule 테이블을 생성한다 (없으면 생성).
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private void createExceptionRuleTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS exception_rule (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_key  TEXT NOT NULL,
                    type       TEXT NOT NULL,
                    pattern    TEXT NOT NULL,
                    memo       TEXT,
                    created_at INTEGER NOT NULL
                );
                """;
        jdbcTemplate.execute(sql);
        log.info("[SettingsRepository] exception_rule 테이블 초기화 완료");
    }

    // ===== 감시 폴더 =====

    /*
     * 함수 이름 : findWatchedFolders
     * 기능 : ownerKey에 해당하는 감시 폴더 목록을 ID 오름차순으로 조회한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : List<WatchedFolder>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<WatchedFolder> findWatchedFolders(String ownerKey) {
        String sql = """
                SELECT id, owner_key, name, path, created_at
                FROM watched_folder
                WHERE owner_key = ?
                ORDER BY id ASC
                """;
        return jdbcTemplate.query(sql, new Object[]{ownerKey}, watchedFolderRowMapper());
    }

    /*
     * 함수 이름 : insertWatchedFolder
     * 기능 : 감시 폴더를 DB에 저장하고 생성된 레코드를 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String name - 폴더 이름, String path - 절대 경로
     * 반환값 : WatchedFolder - 저장된 폴더 객체
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public WatchedFolder insertWatchedFolder(String ownerKey, String name, String path) {
        long now = System.currentTimeMillis();

        String sql = """
                INSERT INTO watched_folder (
                    owner_key, name, path, created_at
                ) VALUES (?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, ownerKey, name, path, now);

        // SQLite 는 last_insert_rowid() 로 마지막 PK 조회 가능
        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return WatchedFolder.builder()
                .id(id)
                .ownerKey(ownerKey)
                .name(name)
                .path(path)
                .createdAt(Instant.ofEpochMilli(now))
                .build();
    }

    /*
     * 함수 이름 : deleteWatchedFolder
     * 기능 : ownerKey와 ID가 일치하는 감시 폴더를 삭제한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, Long id - 폴더 PK
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void deleteWatchedFolder(String ownerKey, Long id) {
        String sql = "DELETE FROM watched_folder WHERE owner_key = ? AND id = ?";
        jdbcTemplate.update(sql, ownerKey, id);
    }

    /*
     * 함수 이름 : watchedFolderRowMapper
     * 기능 : ResultSet 행을 WatchedFolder 객체로 변환하는 RowMapper를 반환한다.
     * 매개변수 : 없음
     * 반환값 : RowMapper<WatchedFolder>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private RowMapper<WatchedFolder> watchedFolderRowMapper() {
        return new RowMapper<>() {
            @Override
            public WatchedFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
                return WatchedFolder.builder()
                        .id(rs.getLong("id"))
                        .ownerKey(rs.getString("owner_key"))
                        .name(rs.getString("name"))
                        .path(rs.getString("path"))
                        .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                        .build();
            }
        };
    }

    // ===== 예외 규칙 =====

    /*
     * 함수 이름 : findExceptionRules
     * 기능 : ownerKey에 해당하는 예외 규칙 목록을 ID 오름차순으로 조회한다.
     * 매개변수 : String ownerKey - 세션 소유자 키
     * 반환값 : List<ExceptionRule>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<ExceptionRule> findExceptionRules(String ownerKey) {
        String sql = """
                SELECT id, owner_key, type, pattern, memo, created_at
                FROM exception_rule
                WHERE owner_key = ?
                ORDER BY id ASC
                """;
        return jdbcTemplate.query(sql, new Object[]{ownerKey}, exceptionRuleRowMapper());
    }

    /*
     * 함수 이름 : insertExceptionRule
     * 기능 : 예외 규칙을 DB에 저장하고 생성된 레코드를 반환한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, String type - 규칙 타입(PATH/EXT/PROCESS), String pattern - 경로 또는 패턴, String memo - 메모
     * 반환값 : ExceptionRule - 저장된 예외 규칙 객체
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public ExceptionRule insertExceptionRule(String ownerKey, String type, String pattern, String memo) {
        long now = System.currentTimeMillis();

        String sql = """
                INSERT INTO exception_rule (
                    owner_key, type, pattern, memo, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, ownerKey, type, pattern, memo, now);

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return ExceptionRule.builder()
                .id(id)
                .ownerKey(ownerKey)
                .type(type)
                .pattern(pattern)
                .memo(memo)
                .createdAt(Instant.ofEpochMilli(now))
                .build();
    }

    /*
     * 함수 이름 : deleteExceptionRule
     * 기능 : ownerKey와 ID가 일치하는 예외 규칙을 삭제한다.
     * 매개변수 : String ownerKey - 세션 소유자 키, Long id - 예외 규칙 PK
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public void deleteExceptionRule(String ownerKey, Long id) {
        String sql = "DELETE FROM exception_rule WHERE owner_key = ? AND id = ?";
        jdbcTemplate.update(sql, ownerKey, id);
    }

    // ====== ADMIN 전용 메서드들 ======

    /*
     * 함수 이름 : countFoldersByOwnerKey
     * 기능 : ownerKey별 감시 폴더 수를 집계하여 반환한다. 관리자 통계용.
     * 매개변수 : 없음
     * 반환값 : List<OwnerKeyStat>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<OwnerKeyStat> countFoldersByOwnerKey() {
        String sql = "SELECT owner_key, COUNT(*) AS cnt FROM watched_folder GROUP BY owner_key";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new OwnerKeyStat(rs.getString("owner_key"), rs.getLong("cnt")));
    }

    /*
     * 함수 이름 : countExceptionsByOwnerKey
     * 기능 : ownerKey별 예외 규칙 수를 집계하여 반환한다. 관리자 통계용.
     * 매개변수 : 없음
     * 반환값 : List<OwnerKeyStat>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public List<OwnerKeyStat> countExceptionsByOwnerKey() {
        String sql = "SELECT owner_key, COUNT(*) AS cnt FROM exception_rule GROUP BY owner_key";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new OwnerKeyStat(rs.getString("owner_key"), rs.getLong("cnt")));
    }

    /*
     * 함수 이름 : countTotalWatchedFolders
     * 기능 : 전체 감시 폴더 수를 반환한다. 관리자 통계용.
     * 매개변수 : 없음
     * 반환값 : long - 전체 감시 폴더 수
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    public long countTotalWatchedFolders() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM watched_folder", Long.class);
        return count == null ? 0 : count;
    }

    public record OwnerKeyStat(String ownerKey, long count) {}

    /*
     * 함수 이름 : exceptionRuleRowMapper
     * 기능 : ResultSet 행을 ExceptionRule 객체로 변환하는 RowMapper를 반환한다.
     * 매개변수 : 없음
     * 반환값 : RowMapper<ExceptionRule>
     * 작성 날짜 : 2026/03/04
     * 작성자 : 이상혁
     */
    private RowMapper<ExceptionRule> exceptionRuleRowMapper() {
        return new RowMapper<>() {
            @Override
            public ExceptionRule mapRow(ResultSet rs, int rowNum) throws SQLException {
                return ExceptionRule.builder()
                        .id(rs.getLong("id"))
                        .ownerKey(rs.getString("owner_key"))
                        .type(rs.getString("type"))
                        .pattern(rs.getString("pattern"))
                        .memo(rs.getString("memo"))
                        .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                        .build();
            }
        };
    }
}
