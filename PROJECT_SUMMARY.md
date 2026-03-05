# WatchService Agent - 통합 프로젝트 전체 정리

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **목적** | 실시간 파일 모니터링 + AI 기반 랜섬웨어 탐지 |
| **아키텍처** | 3-서버 분산 시스템 |
| **데이터베이스** | SQLite (`log.db`) |
| **Git 브랜치** | `main` (clean 상태) |

---

## 2. 전체 디렉토리 구조

```
/Users/sanghyeok/Desktop/Project/통합/
├── watchservice_be/     # Spring Boot 백엔드 (Java 17)
├── watchservice_fe/     # React 프론트엔드 (React 19)
├── 코드/               # FastAPI AI 서버 + 학습 스크립트
├── .git/
├── .vscode/
└── PROJECT_OVERVIEW.md
```

---

## 3. 백엔드 (watchservice_be)

### 기술 스택

- **Spring Boot 3.5.6** / **Java 17** / **Gradle**
- **SQLite** (JdbcTemplate, JPA 없음)
- **Lombok, Spring Actuator**
- **포트:** 8080

### 주요 패키지 구조 (82개 Java 파일)

```
com.watchserviceagent.watchservice_agent/
├── watcher/       # 파일시스템 이벤트 감지
├── collector/     # 파일 분석 (해시, 엔트로피, 크기)
├── analytics/     # 3초 윈도우 집계
├── ai/            # FastAPI 통신 + Gemini LLM
├── alerts/        # 알림 관리
├── storage/       # 로그 저장
├── settings/      # 모니터링 폴더·예외 규칙 관리
├── scan/          # 수동 스캔
├── dashboard/     # 대시보드 요약
├── admin/         # 관리자 인증·공지·피드백
├── support/       # 레거시 지원
└── common/        # CORS, 세션, 공통 유틸
```

### 핵심 서비스 (13개)

| 서비스 | 역할 |
|--------|------|
| `WatcherService` | Java WatchService API로 파일 이벤트 감지 |
| `FileCollectorService` | 파일 해시(SHA-256), 엔트로피, 크기 분석 |
| `EventWindowAggregator` | 3초 단위 이벤트 집계 |
| `AiService` | FastAPI 서버와 REST 통신 |
| `GeminiAdviceService` | Gemini LLM으로 대응 가이드 생성 |
| `NotificationService` | 알림 저장 및 조회 |
| `AlertService` | 로그 기반 AI 결과 관리 |
| `LogService` | 이벤트 로그 영속화 |
| `SettingsService` | 폴더 및 예외 규칙 관리 |
| `ScanService` | 수동 디렉토리 스캔 |
| `DashboardController` | 요약 데이터 집계 |
| `AdminAuthService` | 세션 기반 관리자 인증 |
| `FeedbackService` | 사용자 피드백 수집 |

### DB 테이블 (7개)

| 테이블 | 역할 | 주요 컬럼 |
|--------|------|-----------|
| `log` | 파일 이벤트 + AI 분석 결과 | id, owner_key, event_type, path, hash, entropy, ai_label, ai_score |
| `notification` | 윈도우 집계된 알림 + guidance | id, owner_key, window_start, window_end, ai_label, top_family, guidance |
| `watched_folder` | 모니터링 대상 폴더 | id, owner_key, name, path, created_at |
| `exception_rule` | 제외 패턴 규칙 | id, owner_key, type, pattern, memo, created_at |
| `feedback` | 사용자 피드백 | id, email, content, created_at |
| `notice` | 공지사항 | id, title, content, created_at |
| `feedback_ticket` | 레거시 문의 | id, owner_key, name, email, content |

### 핵심 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:sqlite:log.db
    driver-class-name: org.sqlite.JDBC

ai:
  analyze:
    url: http://localhost:8001/api/analyze
  family:
    url: http://localhost:8001/predict

gemini:
  api-key: ${GEMINI_API_KEY:}
  model: gemini-1.5-flash
  timeout-ms: 2500

admin:
  username: ${ADMIN_USERNAME:admin}
  password: ${ADMIN_PASSWORD:123456789}

watchservice:
  analytics:
    window-ms: 3000
    encrypt:
      entropy-diff-threshold: 0.30
      min-size-bytes: 4096
    random-ext:
      min-count: 2
      whitelist: "txt,log,doc,docx,xls,xlsx,pdf,png,jpg,jpeg,gif,zip,rar,7z"
```

---

## 4. 프론트엔드 (watchservice_fe)

### 기술 스택

- **React 19.2.0** / **react-router-dom 7.9.6**
- **Create React App** / **CSS Modules**
- **포트:** 3000

### 파일 구조 (18개 JS 파일)

```
src/
├── App.js                   # 루트 라우팅 컴포넌트
├── index.js                 # 애플리케이션 진입점
│
├── api/                     # API 클라이언트 레이어 (8개)
│   ├── HttpClient.js        # HTTP 래퍼 (BASE_URL: localhost:8080)
│   ├── DashboardApi.js
│   ├── LogsApi.js
│   ├── NotificationsApi.js
│   ├── WatcherApi.js
│   ├── ScanApi.js
│   ├── SettingApi.js
│   └── AdminApi.js
│
├── hooks/                   # 커스텀 훅 (5개)
│   ├── UseLogs.js
│   ├── UseNotifications.js
│   ├── UseWatchedFolders.js
│   ├── UseExceptions.js
│   └── UseProtectionStatus.js
│
├── layout/
│   └── MainLayout.js
│
└── pages/                   # 페이지 컴포넌트 (9개)
    ├── mainboard/
    ├── notifications/
    ├── logs/
    ├── notice/
    ├── settings/            # 6개 설정 페이지
    └── admin/               # 4개 관리자 페이지
```

### 라우트 (18개)

| 경로 | 페이지 |
|------|--------|
| `/` | 메인 대시보드 |
| `/notifications` | 알림 목록 |
| `/notifications/:id` | 알림 상세 + guidance |
| `/notifications/stats` | 통계 |
| `/logs` | 이벤트 로그 |
| `/notice` | 공지사항 |
| `/settings` | 설정 홈 |
| `/settings/folders` | 모니터링 폴더 관리 |
| `/settings/exceptions` | 예외 규칙 관리 |
| `/settings/notify` | 알림 설정 |
| `/settings/reset` | 초기화 |
| `/settings/update` | 업데이트 관리 |
| `/settings/feedback` | 사용자 피드백 |
| `/admin/login` | 관리자 로그인 |
| `/admin/main` | 관리자 대시보드 |
| `/admin/feedback` | 피드백 관리 |
| `/admin/notification` | 공지 관리 |

### 컴포넌트 계층 구조

```
App.js (Router)
  └── MainLayout
        └── Pages
              ├── MainBoardPage
              ├── LogsPage          → UseLogs 훅
              ├── NotificationPage  → UseNotifications 훅
              ├── SettingFoldersPage → UseWatchedFolders 훅
              ├── SettingExceptionsPage → UseExceptions 훅
              └── AdminPages
```

---

## 5. AI 서버 (코드/api_server.py)

### 기술 스택

- **FastAPI** / **XGBoost** (주) / LightGBM·Sklearn (대체)
- **포트:** 8001
- **학습 데이터:** 합성 랜섬웨어 데이터셋 30,000샘플

### AI Artifacts

```
코드/artifacts/
├── model_xgb.json       # 학습된 XGBoost 모델 (764KB)
├── features.json        # 9개 피처 목록
└── classes.json         # 클래스명 ["benign", "ransomware"]
```

### API 엔드포인트

| 메서드 | 경로 | 역할 |
|--------|------|------|
| `GET` | `/health` | 서버 상태 + 모델 로드 상태 |
| `POST` | `/api/analyze` | **이진 분류** (SAFE / WARNING / DANGER) |
| `POST` | `/predict` | 랜섬웨어 패밀리 분류 (top-k) |
| `GET` | `/debug/feats` | 입력 피처 확인 |

### 요청/응답 형식 (/api/analyze)

```json
// Request
{
  "file_read_count": 5,
  "file_write_count": 12,
  "file_delete_count": 3,
  "file_rename_count": 2,
  "file_encrypt_like_count": 8,
  "changed_files_count": 10,
  "random_extension_flag": 1,
  "entropy_diff_mean": 0.45,
  "file_size_diff_mean": 2048.5
}

// Response
{
  "status": "ok",
  "label": "DANGER",
  "score": 0.87,
  "detail": "top_family=LockBit, top_prob=0.87",
  "topk": [
    {"family": "LockBit", "prob": 0.87},
    {"family": "benign", "prob": 0.13}
  ]
}
```

### 9개 입력 피처

| # | 피처 | 의미 |
|---|------|------|
| 1 | `file_read_count` | 파일 읽기 횟수 |
| 2 | `file_write_count` | 파일 쓰기 횟수 |
| 3 | `file_delete_count` | 파일 삭제 횟수 |
| 4 | `file_rename_count` | 파일 이름 변경 횟수 |
| 5 | `file_encrypt_like_count` | 암호화 의심 횟수 (높은 엔트로피 변화) |
| 6 | `changed_files_count` | 영향받은 총 파일 수 |
| 7 | `random_extension_flag` | 비정상 확장자 감지 여부 (0/1) |
| 8 | `entropy_diff_mean` | 평균 엔트로피 변화량 |
| 9 | `file_size_diff_mean` | 평균 파일 크기 변화량 |

---

## 6. 랜섬웨어 탐지 파이프라인

```
사용자 폴더 선택
    ↓
WatcherService
(Java WatchService API로 파일 이벤트 감지: CREATE / MODIFY / DELETE)
    ↓
FileCollectorService
(SHA-256 해시, 엔트로피, 크기, 확장자 변화 분석)
    ↓
EventWindowAggregator
(3초 단위 이벤트 집계 → 9개 피처 생성)
    ↓
AiService → FastAPI /api/analyze
(XGBoost ML 모델 분류)
    ↓
결과: SAFE / WARNING / DANGER
    ↓ (WARNING / DANGER 일 때)
GeminiAdviceService
(Gemini 1.5 Flash로 대응 가이드 생성, 타임아웃 2.5초)
    ↓
NotificationService + LogService
(DB 저장: notification 테이블, log 테이블)
    ↓
프론트엔드 화면 표시
```

---

## 7. 백엔드 API 명세 (40개+)

### Dashboard
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/dashboard/summary` | 상태, 최근 이벤트, 가이드 |

### Watcher
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST/GET | `/watcher/start?folderPath=<path>` | 모니터링 시작 |
| POST/GET | `/watcher/stop` | 모니터링 중지 |

### Logs
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/logs/recent?limit=10` | 최근 N개 로그 |
| GET | `/logs?page=0&size=20` | 페이지네이션 로그 |
| GET | `/logs/{id}` | 단일 로그 상세 |
| DELETE | `/logs/{id}` | 로그 단건 삭제 |
| POST | `/logs/delete` | 로그 일괄 삭제 |
| POST | `/logs/export` | CSV/JSON 내보내기 |

### Notifications
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/notifications?page=0&size=20` | 알림 목록 |
| GET | `/notifications/{id}` | 알림 상세 + guidance |

### Alerts
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/alerts?page=0&size=20` | AI 결과 포함 알림 |
| GET | `/alerts/{id}` | 상세 |
| GET | `/alerts/stats` | 일별/주별 통계 |

### Settings
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/settings/folders` | 폴더 목록 |
| POST | `/settings/folders` | 폴더 추가 |
| DELETE | `/settings/folders/{id}` | 폴더 제거 |
| GET | `/settings/exceptions` | 예외 규칙 목록 |
| POST | `/settings/exceptions` | 예외 규칙 추가 |
| DELETE | `/settings/exceptions/{id}` | 예외 규칙 제거 |
| GET/PUT | `/settings/notification` | 알림 설정 |
| POST | `/settings/reset` | 초기화 |

### Scan (수동)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/scan/start` | 스캔 시작 |
| POST | `/scan/{scanId}/pause` | 스캔 일시정지 |
| GET | `/scan/{scanId}/progress` | 진행률 조회 |

### Admin (세션 인증 필요)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/admin/login` | 로그인 |
| POST | `/api/admin/logout` | 로그아웃 |
| GET | `/api/admin/feedback` | 피드백 목록 |
| DELETE | `/api/admin/feedback/{id}` | 피드백 삭제 |
| POST | `/api/admin/notifications` | 공지 생성 |
| DELETE | `/api/admin/notifications/{id}` | 공지 삭제 |

### Public (인증 불필요)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications` | 공지 목록 |
| POST | `/api/feedback` | 피드백 제출 |

---

## 8. 보안 구조

| 항목 | 내용 |
|------|------|
| **관리자 인증** | 세션 기반, `AdminAuthInterceptor`로 `/api/admin/**` 보호 |
| **사용자 격리** | `owner_key`로 사용자별 데이터 분리 |
| **CORS** | `localhost:3000`, `localhost:5173` 허용, 자격증명 포함 |
| **Gemini API Key** | 서버 환경변수로만 관리 (프론트 미노출) |
| **관리자 자격증명** | 환경변수 `ADMIN_USERNAME`, `ADMIN_PASSWORD`로 주입 |

---

## 9. 실행 방법

```bash
# 1. AI 서버 (먼저 실행)
cd 코드
python api_server.py
# → http://localhost:8001

# 2. 백엔드 (환경변수 설정 후)
export GEMINI_API_KEY=your_api_key
cd watchservice_be
./gradlew bootRun
# → http://localhost:8080

# 3. 프론트엔드
cd watchservice_fe
npm install   # 최초 1회
npm start
# → http://localhost:3000
```

---

## 10. 통계 요약

| 항목 | 수치 |
|------|------|
| Java 파일 | 82개 |
| JavaScript 파일 | 18개 |
| Python 파일 | 6개 |
| DB 테이블 | 7개 |
| API 엔드포인트 | 40개+ |
| React 라우트 | 18개 |
| 백엔드 서비스 | 13개 |
| 커스텀 훅 | 5개 |
| AI 입력 피처 | 9개 |
| 기술 스택 | 3개 (Java / React / Python) |
