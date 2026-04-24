package com.watchserviceagent.watchservice_agent.audit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 클래스 이름 : AuditRepository
 * 기능 : 파일 권한 감사 결과를 SQLite에 저장하고 조회한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AuditRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS file_audit (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_key   TEXT NOT NULL,
                    file_path   TEXT NOT NULL,
                    permissions TEXT,
                    issue       TEXT,
                    audited_at  INTEGER NOT NULL
                )
                """);
        // 오래된 감사 결과는 최신 1회분만 유지하기 위해 인덱스 생성
        try {
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_file_audit_owner ON file_audit(owner_key, audited_at DESC)");
        } catch (Exception ignore) {}
        log.info("[AuditRepository] file_audit 테이블 초기화 완료");
    }

    /**
     * 함수 이름 : saveAll
     * 기능 : 감사 결과 목록을 일괄 저장한다. 저장 전 해당 ownerKey의 기존 결과를 모두 삭제한다.
     */
    public void saveAll(List<AuditResult> results) {
        if (results.isEmpty()) return;
        String ownerKey = results.get(0).getOwnerKey();
        // 이전 감사 결과 삭제 (최신 1회만 유지)
        jdbcTemplate.update("DELETE FROM file_audit WHERE owner_key = ?", ownerKey);

        String sql = "INSERT INTO file_audit (owner_key, file_path, permissions, issue, audited_at) VALUES (?,?,?,?,?)";
        for (AuditResult r : results) {
            jdbcTemplate.update(sql,
                    r.getOwnerKey(),
                    r.getFilePath(),
                    r.getPermissions(),
                    r.getIssue(),
                    r.getAuditedAt().toEpochMilli());
        }
    }

    /**
     * 함수 이름 : findByOwner
     * 기능 : 해당 사용자의 최신 감사 결과 목록을 반환한다.
     */
    public List<AuditResult> findByOwner(String ownerKey) {
        return jdbcTemplate.query(
                "SELECT id, owner_key, file_path, permissions, issue, audited_at FROM file_audit WHERE owner_key = ? ORDER BY id ASC",
                (rs, n) -> AuditResult.builder()
                        .id(rs.getLong("id"))
                        .ownerKey(rs.getString("owner_key"))
                        .filePath(rs.getString("file_path"))
                        .permissions(rs.getString("permissions"))
                        .issue(rs.getString("issue"))
                        .auditedAt(Instant.ofEpochMilli(rs.getLong("audited_at")))
                        .build(),
                ownerKey
        );
    }
}
