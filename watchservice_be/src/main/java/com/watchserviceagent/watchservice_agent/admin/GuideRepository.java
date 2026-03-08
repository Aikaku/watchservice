package com.watchserviceagent.watchservice_agent.admin;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * 클래스 이름 : GuideRepository
 * 기능 : SQLite guide 테이블에 대한 조회/저장을 제공한다.
 *        guide는 단일 행(id=1)으로 관리되며 UPSERT 방식으로 갱신한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class GuideRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_CONTENT =
            "# 사용 가이드\n\n" +
            "## 개요\n" +
            "WatchService Agent는 지정한 폴더를 실시간으로 감시하여 랜섬웨어 의심 행위를 탐지하고 알림을 보내는 보안 에이전트입니다.\n\n" +
            "## 시작하기\n\n" +
            "### 1. 감시 폴더 등록\n" +
            "설정 > 감시 폴더 메뉴에서 '폴더 추가' 버튼을 클릭한 후, 폴더 탐색 모달에서 감시할 폴더를 선택하고 '이 폴더 선택'을 누르세요.\n\n" +
            "### 2. 메인 보드에서 감시 시작\n" +
            "메인 화면에서 '감시 시작' 버튼을 클릭하면 등록된 폴더에 대한 실시간 감시가 시작됩니다.\n\n" +
            "### 3. 알림 확인\n" +
            "랜섬웨어 의심 행위가 탐지되면 알림 페이지에서 상세 내용을 확인할 수 있습니다. AI 기반 대응 가이드도 함께 제공됩니다.\n\n" +
            "## 주요 기능\n\n" +
            "| 기능 | 설명 |\n" +
            "|------|------|\n" +
            "| 실시간 감시 | 지정 폴더의 파일 생성/수정/삭제 이벤트를 3초 단위로 분석 |\n" +
            "| AI 탐지 | XGBoost 모델 기반 랜섬웨어 행위 패턴 분류 |\n" +
            "| Gemini 대응 가이드 | 탐지 시 Gemini AI가 상황에 맞는 대응 지침 제공 |\n" +
            "| 예외 규칙 | 신뢰하는 프로세스/경로를 화이트리스트에 등록 가능 |\n\n" +
            "## 문의\n" +
            "설정 > 문의/피드백 메뉴를 통해 버그 신고 및 문의를 남길 수 있습니다.";

    @PostConstruct
    public void init() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS guide (
                    id          INTEGER PRIMARY KEY,
                    content     TEXT NOT NULL,
                    updated_at  INTEGER NOT NULL
                );
                """;
        jdbcTemplate.execute(ddl);

        // 기본 가이드 삽입 (최초 1회)
        String insertDefault = "INSERT OR IGNORE INTO guide (id, content, updated_at) VALUES (1, ?, ?)";
        jdbcTemplate.update(insertDefault, DEFAULT_CONTENT, Instant.now().toEpochMilli());

        log.info("[GuideRepository] guide 테이블 초기화 완료");
    }

    public String findContent() {
        String sql = "SELECT content FROM guide WHERE id = 1";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    public void upsertContent(String content) {
        String sql = "UPDATE guide SET content = ?, updated_at = ? WHERE id = 1";
        jdbcTemplate.update(sql, content, Instant.now().toEpochMilli());
    }
}
