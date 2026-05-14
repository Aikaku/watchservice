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

    /*
     * 함수 이름 : init
     * 기능 : 애플리케이션 시작 시 notice 테이블을 생성한다. 이미 존재하면 건너뛴다.
     * 매개변수 : 없음
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

    /*
     * 함수 이름 : insert
     * 기능 : 새 공지사항을 DB에 저장한다.
     * 매개변수 : title - 공지 제목, content - 공지 내용
     * 반환값 : 없음
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public void insert(String title, String content) {
        String sql = "INSERT INTO notice (title, content, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, title, content, Instant.now().toEpochMilli());
    }

    /*
     * 함수 이름 : findAllDesc
     * 기능 : 모든 공지사항을 최신순으로 조회한다.
     * 매개변수 : 없음
     * 반환값 : List<Notice> - 공지사항 목록
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public List<Notice> findAllDesc() {
        String sql = """
                SELECT id, title, content, created_at
                FROM notice
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql, noticeRowMapper());
    }

    /*
     * 함수 이름 : deleteById
     * 기능 : 특정 ID의 공지사항을 삭제한다.
     * 매개변수 : id - 삭제할 공지사항 ID
     * 반환값 : int - 삭제된 행 수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public int deleteById(long id) {
        String sql = "DELETE FROM notice WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    /*
     * 함수 이름 : noticeRowMapper
     * 기능 : ResultSet 행을 Notice 도메인 객체로 변환하는 RowMapper를 반환한다.
     * 매개변수 : 없음
     * 반환값 : RowMapper<Notice> - 행 매핑 함수
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
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

