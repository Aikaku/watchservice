package com.watchserviceagent.watchservice_agent.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 클래스 이름 : SqliteConfig
 * 기능 : 앱 시작 시 SQLite PRAGMA 설정을 초기화한다.
 *        WAL(Write-Ahead Logging) 모드로 전환하여 다중 접근 시 읽기/쓰기 잠금 충돌을 방지한다.
 * 작성 날짜 : 2026/03/09
 * 작성자 : 시스템
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SqliteConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void configurePragmas() {
        // WAL 모드: 읽기와 쓰기가 동시에 가능 — 기본 DELETE 모드 대비 동시성 크게 향상
        String result = jdbcTemplate.queryForObject("PRAGMA journal_mode=WAL", String.class);
        log.info("[SqliteConfig] journal_mode 설정 완료: {}", result);

        // 외래키 제약 활성화 (SQLite 기본값 OFF)
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");
        log.info("[SqliteConfig] foreign_keys=ON 설정 완료");
    }
}
