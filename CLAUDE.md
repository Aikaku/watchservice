# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

랜섬웨어 실시간 탐지 시스템. 두 개의 독립 서브프로젝트로 구성된다.

- `watchservice_be/` — Spring Boot 3.5 / Java 17 / Gradle 백엔드 (에이전트)
- `watchservice_fe/` — React (CRA) 프론트엔드

외부 AI 서버(Python, XGBoost)와 Gemini LLM API에 의존한다.

---

## Commands

### Backend (`watchservice_be/`)

```bash
./gradlew build          # 전체 빌드 (테스트 포함)
./gradlew build -x test  # 테스트 없이 빌드
./gradlew bootRun        # 개발 서버 실행 (포트 8080)
./gradlew test           # 테스트 실행
```

### Frontend (`watchservice_fe/`)

```bash
npm start       # 개발 서버 실행 (포트 3000)
npm run build   # 프로덕션 빌드
npm test        # Jest 테스트 실행
```

---

## Backend Architecture

### 핵심 데이터 흐름

```
WatcherService (Java NIO WatchService)
  → FileCollectorService  (파일 분석: 크기·엔트로피·확장자, 스냅샷 관리)
  → EventWindowAggregator (3초 윈도우 집계 → 9개 피처 → AI 서버 호출)
  → AiService             (POST /api/analyze → 랜섬웨어 판정 + Gemini guidance)
  → LogService.saveAsync() + NotificationService.saveNotification()
```

### 도메인 패키지 역할

| 패키지 | 핵심 클래스 | 역할 |
|--------|------------|------|
| `watcher/` | `WatcherService` | NIO WatchService로 폴더 재귀 감시. CREATE/MODIFY/DELETE 이벤트 발생 |
| `collector/` | `FileCollectorService`, `EntropyAnalyzer`, `FileSnapshotStore` | 파일 크기·엔트로피·확장자 계산. baseline/last 스냅샷을 메모리(ConcurrentHashMap)에 유지 |
| `analytics/` | `EventWindowAggregator` | 3초 윈도우 집계, 9개 XGBoost 피처 계산, AI 서버 호출 조율 |
| `ai/` | `AiService`, `GeminiAdviceService` | 외부 AI 서버 HTTP 통신, Gemini API guidance 생성 |
| `storage/` | `LogService`, `LogWriterWorker`, `LogRepository` | SQLite 비동기 로그 저장(큐 방식), 조회·삭제·CSV/JSON 내보내기 |
| `alerts/` | `NotificationService`, `NotificationRepository` | 윈도우 단위 알림 저장·조회 |
| `settings/` | `SettingsService`, `SettingsRepository` | 감시 폴더·예외 규칙 CRUD |
| `admin/` | `AdminAuthInterceptor`, Admin*Controller | 세션 기반 관리자 인증. `/api/admin/**` 보호 |
| `common/` | `OwnerKeyUtil` | HTTP 세션마다 고유 `ownerKey` 발급 (`HttpSession` 속성 `"OWNER_KEY"`) |

### AI 피처 벡터 (AiPayload — 9개)

`EventWindowAggregator.computeWindowStats()`가 계산. 필드명 변경 금지 (AI 서버 API 호환).

| 필드 | 설명 |
|------|------|
| `fileReadCount` | 세션 기반 파일 접근 수 (touchSessionTimeoutMs=5분 기준) |
| `fileWriteCount` | 내용 변경(크기/엔트로피 변화) 기반 쓰기 수 |
| `fileDeleteCount` | DELETE 이벤트 수 (rename 보정 후) |
| `fileRenameCount` | DELETE+CREATE 점수 매칭으로 탐지된 rename 수 |
| `fileEncryptLikeCount` | 엔트로피 증가+크기·확장자 변화, 또는 절대 엔트로피 ≥7.2 |
| `changedFilesCount` | 변경된 고유 파일 수 |
| `randomExtensionFlag` | suspicious 확장자 파일 ≥2 또는 동일 확장자 ≥3이면 1 |
| `entropyDiffMean` | |diff|>0.05인 유의미한 엔트로피 변화 평균 |
| `fileSizeDiffMean` | 크기 변화 평균 (bytes) |

### 엔트로피 샘플링 (`EntropyAnalyzer`)

- `computeMultiSectionEntropy(path, 4096)`: 파일 크기 > 4096이면 앞 40% / 중간 40% / 끝 20% 3구간 샘플링 (`SeekableByteChannel`)
- `computeSampleEntropy(path, maxBytes)`: 앞부분 순차 읽기 (소형 파일용)

### 스냅샷 저장소 (`FileSnapshotStore`)

- `baseline`: 최초 관측 상태 (putIfAbsent)
- `last`: 직전 이벤트 상태 (항상 갱신)
- `before` = last → baseline 순으로 폴백하여 `FileCollectorService.analyze()`에서 diff 계산

### 관리자 인증

- `AdminAuthInterceptor`가 `/api/admin/**` 요청을 가로챔 (단, `/api/admin/login` 제외)
- HTTP 세션의 `ADMIN_AUTH` 속성 = `true`이면 통과
- 계정: 환경변수 `ADMIN_USERNAME` / `ADMIN_PASSWORD` (기본값: `admin` / `123456789`)

### 외부 서비스 엔드포인트 (`application.yml`)

```yaml
ai.analyze.url: http://localhost:8001/api/analyze   # XGBoost 행위 분석
ai.family.url:  http://localhost:8001/predict        # 랜섬웨어 패밀리 분류
gemini.api-key: ${GEMINI_API_KEY:}                  # Gemini guidance (선택)
```

DB는 프로젝트 루트의 `log.db` (SQLite). JPA 미사용, Spring JDBC 직접 사용.

---

## Frontend Architecture

### API 통신

`src/api/HttpClient.js`가 모든 API 요청의 기반. `REACT_APP_API_BASE_URL` 환경변수로 백엔드 주소 설정 (기본: `http://localhost:8080`).

도메인별 래퍼: `WatcherApi.js`, `LogsApi.js`, `NotificationsApi.js`, `SettingApi.js`, `AdminApi.js`, `DashboardApi.js`, `ScanApi.js`

### 라우트 구조

- `/` — 메인 대시보드 (`MainBoardPage`)
- `/notifications`, `/notifications/:id`, `/notifications/stats` — 알림
- `/logs` — 이벤트 로그
- `/settings/**` — 감시 폴더·예외 규칙·알림 설정
- `/notice` — 공지사항(사용자)
- `/admin/**` — 관리자 페이지 (로그인 필요)

레이아웃: `MainLayout` (헤더 + `NavSidebar` + 콘텐츠 영역)

### 관리자 페이지 인증

`AdminLoginPage`에서 `adminLogin()` 호출 → 백엔드 세션 생성. 이후 모든 `AdminApi` 호출에 세션 쿠키 자동 포함 (`credentials: 'include'` 불필요 — `fetch` 기본 same-origin 동작).

---

## Work Log Policy

작업이 끝나면 반드시 당일 날짜 이름의 마크다운 파일에 작업 내용을 정리한다.

- **파일 위치**: 프로젝트 루트 (`/Users/sanghyeok/Desktop/Project/통합/`)
- **파일명 형식**: `YYYY-MM-DD.md` (예: `2026-03-06.md`)
- **기록 내용**: 변경한 파일 목록, 작업 내용, 변경 이유, 빌드 결과
- 파일이 이미 존재하면 새 섹션을 추가한다. 덮어쓰지 않는다.

---

## Key Constraints

- `AiPayload` 필드명 변경 금지 — AI 서버 API 호환성
- `watchservice.analytics.*` 설정값들은 `@Value`로 주입되며 `application.yml`에 기본값 정의됨. 새 설정 추가 시 `@Value` 어노테이션과 yml 양쪽 모두 수정
- `SCAN` 이벤트 타입은 `EventWindowAggregator`의 윈도우 집계에서 제외됨 (로그만 저장)
- 랜섬웨어 패밀리 분류 목록은 `AiService.classifyFamilyCategory()`에 하드코딩됨
- `FileSnapshotStore`는 Spring Bean이 아님 — `SnapshotConfig`를 통해 `@Bean`으로 등록됨
