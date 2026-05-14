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

    private static final String DEFAULT_CONTENT = """
            # WatchService Agent 사용 가이드

            WatchService Agent는 지정한 폴더를 실시간으로 감시하여 랜섬웨어 의심 행위를 탐지하고 알림을 보내는 보안 에이전트입니다.

            ---

            ## 빠른 시작 (3단계)

            ### 1단계 — 감시 폴더 등록

            1. 왼쪽 메뉴에서 **설정 → 감시 폴더**로 이동합니다
            2. **폴더 추가** 버튼을 클릭합니다
            3. 탐색기에서 보호할 폴더를 선택하고 확인을 클릭합니다
            4. 목록에 폴더가 추가된 것을 확인합니다

            > 중요한 문서, 사진, 업무 파일이 있는 폴더를 등록하세요.

            ### 2단계 — 즉시 검사로 기준 상태 등록

            1. 왼쪽 메뉴에서 **대시보드**로 이동합니다
            2. **즉시 검사** 버튼을 클릭합니다
            3. 진행률 바가 100%가 될 때까지 기다립니다
            4. 완료 팝업에서 검사된 파일 수를 확인하고 **확인**을 클릭합니다
            5. 자동으로 실시간 감시가 시작됩니다

            > 즉시 검사는 현재 파일 상태를 기준으로 저장합니다. 이후 변화를 이 기준과 비교하여 이상 징후를 탐지합니다.

            ### 3단계 — 알림 확인

            탐지가 발생하면 다음과 같이 동작합니다.

            1. 대시보드 상단 배지가 **WARNING** 또는 **DANGER** 로 변경됩니다
            2. DANGER 탐지 시 전체화면 긴급 경보 팝업이 표시됩니다
            3. 왼쪽 메뉴 **알림**에서 탐지 이력과 AI 대응 가이드를 확인합니다

            ---

            ## 주요 기능 사용법

            ### 알림 상세 조회

            1. 왼쪽 메뉴에서 **알림**을 클릭합니다
            2. 목록에서 확인할 알림을 클릭합니다
            3. 상세 페이지에서 다음 정보를 확인할 수 있습니다
               - 탐지 시각, 위험도(SAFE / WARNING / DANGER), AI 점수
               - 영향을 받은 파일 경로 목록
               - Gemini AI가 생성한 단계별 대응 가이드

            ### 이메일 알림 설정

            DANGER 탐지 시 지정한 이메일로 경보 메일을 자동 발송합니다.

            1. 왼쪽 메뉴에서 **설정 → 이메일 알림**으로 이동합니다
            2. 수신할 이메일 주소를 입력하고 **저장**을 클릭합니다
            3. **테스트 발송** 버튼으로 설정이 정상 동작하는지 확인합니다
            4. 이후 DANGER 탐지 시 자동으로 경보 메일이 발송됩니다

            > 테스트 메일이 오지 않으면 스팸함을 확인하세요.

            ### 예외 규칙 등록 (오탐 방지)

            자주 변경되는 정상 파일이 반복적으로 경보를 울릴 때 예외로 등록할 수 있습니다.

            1. 왼쪽 메뉴에서 **설정 → 예외 관리**로 이동합니다
            2. **예외 추가** 버튼을 클릭합니다
            3. 유형을 선택합니다
               - **EXTENSION**: 특정 확장자 제외 (예: .tmp, .log)
               - **PATH**: 특정 경로 제외 (예: C:\\\\Users\\\\사용자\\\\AppData)
            4. 패턴을 입력하고 **추가**를 클릭합니다

            또는 알림 상세 페이지에서 **오탐 신고** 버튼을 클릭하면 해당 경로가 자동으로 예외 규칙에 등록됩니다.

            ### 감시 스케줄 설정

            특정 시간대에만 감시가 활성화되도록 예약할 수 있습니다.

            1. 왼쪽 메뉴에서 **설정 → 감시 스케줄**로 이동합니다
            2. 스케줄 활성화 토글을 켭니다
            3. 감시할 요일과 시작·종료 시간을 설정합니다
            4. **저장**을 클릭합니다

            > 스케줄 설정 시 지정된 시간 외에는 파일 이벤트가 발생해도 AI 분석이 실행되지 않습니다.

            ### 이벤트 로그 확인

            1. 왼쪽 메뉴에서 **로그**를 클릭합니다
            2. 검색창에 파일명이나 경로를 입력해 필터링할 수 있습니다
            3. 로그 항목을 클릭하면 파일 경로, 이벤트 유형, 엔트로피 변화 등 상세 정보를 확인합니다
            4. **CSV 내보내기** 또는 **JSON 내보내기**로 로그를 파일로 저장할 수 있습니다

            ### 탐지 리포트 PDF 생성

            1. 왼쪽 메뉴에서 **알림 → 통계**로 이동합니다
            2. 페이지 하단 **PDF 리포트 내보내기** 섹션으로 스크롤합니다
            3. 시작일과 종료일을 입력합니다 (빈칸이면 전체 기간)
            4. **PDF 다운로드** 버튼을 클릭합니다

            ---

            ## 탐지 등급 설명

            | 등급 | 색상 | 의미 | 권장 조치 |
            |------|------|------|-----------|
            | **SAFE** | 초록 | 정상 범위 | 모니터링 유지 |
            | **WARNING** | 노랑 | 의심 행위 감지 | 알림 상세 확인 후 판단 |
            | **DANGER** | 빨강 | 랜섬웨어 의심 행위 | 즉시 네트워크 차단 및 대응 가이드 확인 |

            ---

            ## 자주 묻는 질문

            **Q. 앱을 켜면 자동으로 감시가 시작되나요?**
            아니요. 앱 실행 후 대시보드에서 **감시 시작** 또는 **즉시 검사** 버튼을 클릭해야 합니다. 설정에서 자동 실행을 활성화하면 PC 부팅 시 앱이 자동으로 실행됩니다.

            **Q. 정상 파일인데 경보가 울렸어요.**
            알림 상세 페이지에서 **오탐 신고** 버튼을 클릭하세요. 해당 경로가 예외 규칙에 자동 등록되어 이후 탐지에서 제외됩니다.

            **Q. AI 서버에 연결할 수 없다는 메시지가 표시됩니다.**
            인터넷 연결 상태를 확인하세요. AI 분석 서버는 클라우드에서 운영되며 인터넷 연결이 필요합니다.

            **Q. 이메일 테스트 발송이 실패해요.**
            수신 이메일 주소가 올바른지 확인하세요. 문제가 지속되면 설정 → 문의/피드백으로 신고해 주세요.

            ---

            ## 문의 및 피드백

            불편한 점이나 버그를 발견하셨나요?

            1. 왼쪽 메뉴에서 **설정 → 문의/피드백**으로 이동합니다
            2. 이름, 이메일, 내용을 입력하고 **전송**을 클릭합니다
            3. 관리자가 확인 후 이메일로 답변드립니다
            """;

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
