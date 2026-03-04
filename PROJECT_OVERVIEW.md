# WatchService 통합 프로젝트 — 전체 분석 정리본

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **목적** | 데스크톱 파일 감시 + 랜섬웨어 실시간 탐지 에이전트 |
| **구성** | Spring Boot 백엔드 + React 프론트엔드 + FastAPI AI 서버 + (선택) Gemini LLM |
| **저장소** | SQLite 단일 DB (`log.db`) |
| **인증** | 관리자: 세션 기반 (admin ID/PW), 사용자: 세션 식별자(ownerKey) |

---

## 2. 디렉터리 구조

```
통합/
├── watchservice_be/          # Spring Boot 백엔드 (Java 17)
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../watchservice_agent/
│       │   ├── WatchserviceAgentApplication.java
│       │   ├── admin/         # 관리자 인증·피드백·공지
│       │   ├── ai/            # FastAPI 연동 + Gemini 가이던스
│       │   ├── alerts/        # 알림(윈도우 단위 AI 결과)
│       │   ├── analytics/     # 이벤트 윈도우 집계
│       │   ├── collector/     # 파일 분석(엔트로피·스냅샷)
│       │   ├── common/        # WebConfig, SessionIdManager
│       │   ├── dashboard/     # 대시보드 요약
│       │   ├── scan/          # 수동 스캔
│       │   ├── settings/      # 감시 폴더·예외 규칙
│       │   ├── storage/       # 로그 저장·조회
│       │   ├── support/       # 피드백 티켓(레거시)
│       │   └── watcher/       # 파일 감시 시작/중지
│       └── resources/
│           └── application.yml
├── watchservice_fe/          # React SPA (React 19, react-router-dom 7)
│   ├── package.json
│   └── src/
│       ├── api/               # API 래퍼 (HttpClient, *Api.js)
│       ├── components/        # 공통·로그·알림·보호·폴더
│       ├── hooks/             # UseLogs, UseNotifications 등
│       ├── layout/            # MainLayout
│       ├── pages/             # mainboard, logs, notifications, settings, admin, notice
│       └── styles/
└── 코드/                     # AI 서버 및 학습 스크립트
    ├── api_server.py         # FastAPI 랜섬웨어 분류 서버
    ├── untitled21.py          # XGBoost 학습 예시
    └── artifacts/             # 모델·피처·클래스 (model_xgb.json, features.json, classes.json 등)
```

---

## 3. 기술 스택

### 3.1 백엔드 (watchservice_be)

| 구분 | 기술 |
|------|------|
| 프레임워크 | Spring Boot 3.5.6 |
| JDK | 17 |
| 빌드 | Gradle |
| DB | SQLite (org.xerial:sqlite-jdbc 3.46.0.0) |
| 데이터 접근 | Spring JDBC (JdbcTemplate), 스키마는 @PostConstruct에서 CREATE TABLE |
| 기타 | Lombok, Actuator |

### 3.2 프론트엔드 (watchservice_fe)

| 구분 | 기술 |
|------|------|
| UI | React 19.2 |
| 라우팅 | react-router-dom 7.9 |
| 빌드 | react-scripts 5.0 (CRA) |
| 스타일 | CSS (layout.css, component.css) |

### 3.3 AI·외부 연동

| 구분 | 기술 |
|------|------|
| 분류 서버 | FastAPI (Python), XGBoost/LightGBM |
| LLM 가이던스 | Google Gemini API (RestTemplate 호출) |
| 피처 | 9개 (파일 읽기/쓰기/삭제/이름변경/암호화유사/변경파일수/랜덤확장자 플래그/엔트로피·크기 차이 평균) |

---

## 4. 데이터베이스 스키마 (SQLite, log.db)

### 4.1 log

파일 이벤트 + AI 분석 결과 저장.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INTEGER PK | 자동 증가 |
| owner_key | TEXT | 세션 식별자 |
| event_type | TEXT | CREATE / MODIFY / DELETE 등 |
| path | TEXT | 파일 경로 |
| exists_flag | INTEGER | 존재 여부 |
| size | INTEGER | 크기 |
| size_before, size_after | INTEGER | 이전/이후 크기 |
| entropy_before, entropy_after | REAL | 이전/이후 엔트로피 |
| ext_before, ext_after | TEXT | 확장자 |
| exists_before, size_diff, entropy_diff | INTEGER/REAL | 차이 값 |
| last_modified_time | INTEGER | epoch ms |
| hash, entropy | TEXT/REAL | 해시·엔트로피 |
| ai_label | TEXT | SAFE / WARNING / DANGER / UNKNOWN |
| ai_score | REAL | 위험도 0~1 |
| ai_detail | TEXT | AI 상세 메시지 |
| collected_at | INTEGER | 수집 시각 (epoch ms) |

### 4.2 notification

윈도우 단위 AI 분석 결과(알림).

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | INTEGER PK | 자동 증가 |
| owner_key | TEXT | 세션 식별자 |
| window_start, window_end | INTEGER | 윈도우 구간 (epoch ms) |
| created_at | INTEGER | 생성 시각 |
| ai_label, ai_score | TEXT/REAL | AI 라벨·점수 |
| top_family | TEXT | 상위 패밀리 (Benign, LockBit 등) |
| ai_detail | TEXT | 상세 메시지 |
| guidance | TEXT | Gemini 대응 가이던스 |
| affected_files_count | INTEGER | 영향 파일 수 |
| affected_paths | TEXT | JSON 배열 (경로 목록) |

### 4.3 watched_folder

감시 대상 폴더.

| 컬럼 | 타입 |
|------|------|
| id | INTEGER PK |
| owner_key | TEXT |
| name | TEXT |
| path | TEXT |
| created_at | INTEGER |

### 4.4 exception_rule

예외 규칙(패턴·타입·메모).

| 컬럼 | 타입 |
|------|------|
| id | INTEGER PK |
| owner_key | TEXT |
| type | TEXT |
| pattern | TEXT |
| memo | TEXT |
| created_at | INTEGER |

### 4.5 feedback (관리자용 피드백)

| 컬럼 | 타입 |
|------|------|
| id | INTEGER PK |
| email | TEXT |
| content | TEXT |
| created_at | INTEGER |

### 4.6 notice (공지사항)

| 컬럼 | 타입 |
|------|------|
| id | INTEGER PK |
| title | TEXT |
| content | TEXT |
| created_at | INTEGER |

### 4.7 feedback_ticket (레거시 지원)

| 컬럼 | 타입 |
|------|------|
| id | INTEGER PK |
| owner_key | TEXT |
| name | TEXT |
| email | TEXT |
| content | TEXT |
| created_at | INTEGER |

---

## 5. 백엔드 API 목록

### 5.1 대시보드

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /dashboard/summary | 상태·최근 이벤트·감시 경로·guidance 등 요약 |

### 5.2 감시 제어

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST/GET | /watcher/start?folderPath= | 감시 시작 |
| POST/GET | /watcher/stop | 감시 중지 |

### 5.3 스캔

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /scan/start | 스캔 시작 (body: paths, autoStartWatcher 등) |
| POST | /scan/{scanId}/pause | 스캔 일시 중지 |
| GET | /scan/{scanId}/progress | 스캔 진행률 |

### 5.4 로그

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /logs/recent?limit= | 최근 로그 N건 |
| GET | /logs | 페이지·필터·정렬 |
| GET | /logs/{id} | 단건 상세 |
| DELETE | /logs/{id} | 단건 삭제 |
| POST | /logs/delete | 일괄 삭제 (body: ids) |
| POST | /logs/export | CSV/JSON 내보내기 |

### 5.5 알림(윈도우 단위)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /notifications | 알림 목록 (페이지·필터·정렬) |
| GET | /notifications/{id} | 알림 상세 (guidance 포함) |

### 5.6 알림(로그 기반, alerts)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /alerts | ai_label 있는 로그 목록 |
| GET | /alerts/{id} | 단건 |
| GET | /alerts/stats | 일별/주별 통계 |

### 5.7 설정

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /settings/folders | 감시 폴더 목록 |
| POST | /settings/folders | 폴더 추가 (name, path) |
| DELETE | /settings/folders/{id} | 폴더 삭제 |
| GET | /settings/folders/pick | 폴더 선택 다이얼로그(백엔드 Swing) |
| GET | /settings/exceptions | 예외 규칙 목록 |
| POST | /settings/exceptions | 예외 규칙 추가 |
| DELETE | /settings/exceptions/{id} | 예외 규칙 삭제 |
| GET/PUT | /settings/notification | 알림 설정 |
| POST | /settings/reset | 설정 초기화 |

### 5.8 AI (테스트/직접 호출)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /ai/analyze | FastAPI에 AiPayload 전송 후 AiResult 반환 |
| POST | /ai/family/predict | 패밀리 분류 (AiPayload + topk) |
| POST | /ai/family/predict/features | 패밀리 분류 (Map features + topk) |
| GET | /ai/ping | 헬스체크 |

### 5.9 사용자 피드백·공지(비인증)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/feedback | 피드백 저장 (email, content) |
| GET | /api/notifications | 공지사항 목록 (읽기 전용) |

### 5.10 관리자 전용 (세션 인증 필요)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/admin/login | 로그인 (username, password) |
| POST | /api/admin/logout | 로그아웃 |
| GET | /api/admin/feedback | 피드백 목록 |
| DELETE | /api/admin/feedback/{id} | 피드백 삭제 |
| POST | /api/admin/notifications | 공지 등록 (title, content) |
| DELETE | /api/admin/notifications/{id} | 공지 삭제 |

### 5.11 지원(레거시)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /support/feedback | feedback_ticket 테이블에 저장 |

---

## 6. 핵심 데이터 흐름 (랜섬웨어 탐지 파이프라인)

1. **감시 시작**  
   사용자가 감시 폴더 선택 후 “감시 시작” → `WatcherController` → `WatcherService.startWatching(folderPath)` → Java WatchService로 디렉터리 감시.

2. **파일 이벤트**  
   CREATE/MODIFY/DELETE 발생 → `WatcherService`가 `WatcherEventRecord` 생성 → `FileCollectorService.analyze(event)` 호출.

3. **파일 분석**  
   `FileCollectorService`가 `FileSnapshotStore`(baseline/last)와 비교해 크기·엔트로피·확장자 변화 계산 → `FileAnalysisResult` 반환.

4. **윈도우 집계**  
   `EventWindowAggregator.onFileAnalysisResult(result)`가 이벤트를 버퍼에 누적. `windowMs`(기본 3초) 경과 시 `flushWindow()` 실행.

5. **피처 생성**  
   `computeWindowStats()`로 9개 피처 계산 → `AiPayload` 생성 (fileReadCount, fileWriteCount, fileDeleteCount, fileRenameCount, fileEncryptLikeCount, changedFilesCount, randomExtensionFlag, entropyDiffMean, fileSizeDiffMean).

6. **AI 판정**  
   `AiService.requestAnalysis(payload)` → FastAPI `POST /api/analyze` 호출 → 응답을 `AiResult`로 변환.

7. **가이던스 생성**  
   `GeminiAdviceService.guidanceFor(payload, result)`  
   - SAFE: 고정 문장 한 줄 반환.  
   - WARNING/DANGER: Gemini API 호출 후 반환 텍스트를 guidance로 사용.  
   - 실패/타임아웃: fallback 체크리스트 반환.

8. **저장**  
   - 각 `FileAnalysisResult`에 AiResult(guidance 포함) 부착 → `LogService.saveAsync()` → `log` 테이블.  
   - 윈도우 단위 `Notification`(guidance 포함) → `NotificationService.saveNotification()` → `notification` 테이블.

9. **프론트**  
   대시보드·알림·로그 API로 위 데이터 조회 후 표시.

---

## 7. 설정 (application.yml)

| 키 | 설명 | 기본값 |
|----|------|--------|
| spring.datasource.url | SQLite DB 파일 | jdbc:sqlite:log.db |
| ai.analyze.url | FastAPI 분석 URL | http://localhost:8001/api/analyze |
| ai.family.url | FastAPI 패밀리 예측 URL | http://localhost:8001/predict |
| gemini.api-key | Gemini API 키 | ${GEMINI_API_KEY:} |
| gemini.model | Gemini 모델명 | gemini-1.5-flash |
| gemini.timeout-ms | Gemini 타임아웃(ms) | 2500 |
| admin.username | 관리자 ID | ${ADMIN_USERNAME:admin} |
| admin.password | 관리자 PW | ${ADMIN_PASSWORD:123456789} |
| watchservice.analytics.window-ms | 집계 윈도우(ms) | 3000 |
| watchservice.analytics.touch-session-timeout-ms | 터치 세션 타임아웃 | 300000 |
| watchservice.analytics.rename-max-gap-ms | rename 매칭 최대 간격 | 2000 |
| watchservice.analytics.encrypt.* | 엔트로피·크기 임계값 등 | (코드 참고) |
| watchservice.analytics.random-ext.* | 랜덤 확장자 판정·화이트리스트 | (코드 참고) |

---

## 8. 프론트엔드 라우팅 및 페이지

| 경로 | 페이지 | 비고 |
|------|--------|------|
| / | MainBoardPage | 대시보드·감시 제어·최근 이벤트 |
| /notifications | NotificationPage | 알림 목록 |
| /notifications/stats | NotificationStatsPage | 알림 통계 |
| /notifications/:id | NotificationDetailPage | 알림 상세(guidance 표시) |
| /logs | LogsPage | 로그 목록·필터·상세 |
| /notice | UserNoticePage | 공지사항 읽기 전용 |
| /settings | SettingHomePage | 설정 홈 |
| /settings/folders | SettingFoldersPage | 감시 폴더 |
| /settings/exceptions | SettingExceptionsPage | 예외 규칙 |
| /settings/notify | SettingNotifyPage | 알림 설정 |
| /settings/reset | SettingResetPage | 초기화 |
| /settings/update | SettingUpdatePage | 업데이트 |
| /settings/feedback | SettingFeedbackPage | 피드백 전송 |
| /admin/login | AdminLoginPage | 관리자 로그인 |
| /admin/main | AdminMainPage | 관리자 메인 |
| /admin/feedback | AdminFeedbackPage | 피드백 관리 |
| /admin/notification | AdminNoticePage | 공지 관리 |

---

## 9. 프론트엔드 API 모듈·훅

| 파일 | 역할 |
|------|------|
| HttpClient.js | get/post/put/del, BASE_URL |
| DashboardApi.js | fetchDashboardSummary |
| LogsApi.js | fetchRecentLogs, fetchLogs, fetchLogDetail, deleteLog(s), exportLogs |
| NotificationsApi.js | fetchAlerts, fetchAlertDetail, fetchAlertStats |
| ScanApi.js | startScan, pauseScan, fetchScanProgress |
| SettingApi.js | 감시 폴더·예외·알림설정·리셋·sendFeedback |
| WatcherApi.js | startWatcher, stopWatcher |
| AdminApi.js | adminLogin, adminLogout, fetchAdminFeedback, deleteAdminFeedback, fetchNotices, createNotice, deleteNotice |
| UseLogs.js | 로그 목록·필터·페이지 |
| UseNotifications.js | 알림 목록·필터·페이지 |
| UseWatchedFolders.js | 감시 폴더 CRUD·pickFolderPath |
| UseExceptions.js | 예외 규칙 CRUD |
| UseProtectionStatus.js | 보호 상태 관련 |

---

## 10. 보안·인증 요약

- **CORS**  
  `WebConfig`: `http://localhost:3000`, `http://localhost:5173` 허용, credentials true.

- **관리자**  
  `AdminAuthInterceptor`가 `/api/admin/**` 요청 검사. `/api/admin/login` 제외.  
  세션 속성 `ADMIN_AUTH == true` 여부로 통과/401(JSON) 처리.

- **일반 사용자**  
  `SessionIdManager`로 ownerKey 생성·유지. 로그·알림·설정은 owner_key 기준으로 격리.

- **Gemini API 키**  
  서버 환경변수만 사용. 프론트/클라이언트에 노출하지 않음.

---

## 11. 외부 서비스 의존성

| 서비스 | 용도 | 비고 |
|--------|------|------|
| FastAPI (코드/api_server.py) | 9개 피처 → benign/ransomware 판정, 패밀리 분류 | 기본 포트 8001, artifacts 로드 |
| Google Gemini API | WARNING/DANGER 시 대응 가이던스 생성 | 키·타임아웃은 application.yml/환경변수 |

---

## 12. 실행 방법 요약

1. **SQLite DB**  
   백엔드 기동 시 `log.db`가 없으면 생성. 테이블은 각 Repository `@PostConstruct`에서 생성.

2. **백엔드**  
   `cd watchservice_be && ./gradlew bootRun`  
   (선택) `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `GEMINI_API_KEY` 등 설정.

3. **AI 서버**  
   `cd 코드 && python api_server.py` (또는 uvicorn).  
   `OUTPUT_DIR`으로 artifacts 디렉터리 지정.

4. **프론트**  
   `cd watchservice_fe && npm install && npm start`  
   기본 `http://localhost:3000`, API는 `REACT_APP_API_BASE_URL` 또는 `http://localhost:8080`.

---

이 문서는 프로젝트 루트의 `PROJECT_OVERVIEW.md`로 저장되었습니다. 코드 변경 시 함께 갱신하면 유지보수에 도움이 됩니다.
