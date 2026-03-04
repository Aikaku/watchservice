package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.domain.Notice;
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
 * 클래스 이름 : NoticeRepository
 * 기능 : SQLite notice 테이블에 대한 CRUD를 제공한다.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class NoticeRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS notice (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    title       TEXT,
                    content     TEXT NOT NULL,
                    created_at  INTEGER NOT NULL
                );
                """;
        jdbcTemplate.execute(ddl);
        log.info("[NoticeRepository] notice 테이블 초기화 완료");
    }

    public void insert(String title, String content) {
        String sql = "INSERT INTO notice (title, content, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, title, content, Instant.now().toEpochMilli());
    }

    public List<Notice> findAllDesc() {
        String sql = """
                SELECT id, title, content, created_at
                FROM notice
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, noticeRowMapper());
    }

    public int deleteById(long id) {
        String sql = "DELETE FROM notice WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    private RowMapper<Notice> noticeRowMapper() {
        return new RowMapper<>() {
            @Override
            public Notice mapRow(ResultSet rs, int rowNum) throws SQLException {
                return Notice.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .content(rs.getString("content"))
                        .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                        .build();
            }
        };
    }
}

