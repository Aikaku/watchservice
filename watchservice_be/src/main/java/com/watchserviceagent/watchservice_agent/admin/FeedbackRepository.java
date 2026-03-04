package com.watchserviceagent.watchservice_agent.admin;

import com.watchserviceagent.watchservice_agent.admin.domain.Feedback;
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
 * 클래스 이름 : FeedbackRepository
 * 기능 : SQLite feedback 테이블에 대한 CRUD를 제공한다.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class FeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS feedback (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    email       TEXT,
                    content     TEXT NOT NULL,
                    created_at  INTEGER NOT NULL
                );
                """;
        jdbcTemplate.execute(ddl);
        log.info("[FeedbackRepository] feedback 테이블 초기화 완료");
    }

    public void insert(String email, String content) {
        String sql = "INSERT INTO feedback (email, content, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, email, content, Instant.now().toEpochMilli());
    }

    public List<Feedback> findAllDesc() {
        String sql = """
                SELECT id, email, content, created_at
                FROM feedback
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, feedbackRowMapper());
    }

    public int deleteById(long id) {
        String sql = "DELETE FROM feedback WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    private RowMapper<Feedback> feedbackRowMapper() {
        return new RowMapper<>() {
            @Override
            public Feedback mapRow(ResultSet rs, int rowNum) throws SQLException {
                return Feedback.builder()
                        .id(rs.getLong("id"))
                        .email(rs.getString("email"))
                        .content(rs.getString("content"))
                        .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                        .build();
            }
        };
    }
}

