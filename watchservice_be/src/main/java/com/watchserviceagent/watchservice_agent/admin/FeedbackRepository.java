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

    /*
     * 함수 이름 : init
     * 기능 : 애플리케이션 시작 시 feedback 테이블을 생성한다. 이미 존재하면 건너뛴다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : insert
     * 기능 : 새 피드백을 DB에 저장한다.
     * 매개변수 : email - 제출자 이메일, content - 피드백 내용
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public void insert(String email, String content) {
        String sql = "INSERT INTO feedback (email, content, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, email, content, Instant.now().toEpochMilli());
    }

    /*
     * 함수 이름 : findAllDesc
     * 기능 : 모든 피드백을 최신순으로 조회한다.
     * 매개변수 : 없음
     * 반환값 : List<Feedback> - 피드백 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public List<Feedback> findAllDesc() {
        String sql = """
                SELECT id, email, content, created_at
                FROM feedback
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, feedbackRowMapper());
    }

    /*
     * 함수 이름 : deleteById
     * 기능 : 특정 ID의 피드백을 삭제한다.
     * 매개변수 : id - 삭제할 피드백 ID
     * 반환값 : int - 삭제된 행 수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public int deleteById(long id) {
        String sql = "DELETE FROM feedback WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    /*
     * 함수 이름 : feedbackRowMapper
     * 기능 : ResultSet 행을 Feedback 도메인 객체로 변환하는 RowMapper를 반환한다.
     * 매개변수 : 없음
     * 반환값 : RowMapper<Feedback> - 행 매핑 함수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

