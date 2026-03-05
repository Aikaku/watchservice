# WatchService Agent — 심층 기술 보고서

> 작성일: 2026-03-05
> 분석 대상: `/Users/sanghyeok/Desktop/Project/통합`

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [핵심 데이터 흐름 — 완전 추적](#3-핵심-데이터-흐름--완전-추적)
4. [백엔드 상세 분석](#4-백엔드-상세-분석)
5. [AI 서버 상세 분석](#5-ai-서버-상세-분석)
6. [프론트엔드 상세 분석](#6-프론트엔드-상세-분석)
7. [데이터베이스 스키마](#7-데이터베이스-스키마)
8. [전체 API 명세](#8-전체-api-명세)
9. [보안 · 인증 구조](#9-보안--인증-구조)
10. [설정값 레퍼런스](#10-설정값-레퍼런스)
11. [ML 모델 학습 파이프라인](#11-ml-모델-학습-파이프라인)
12. [알려진 이슈 및 기술 부채](#12-알려진-이슈-및-기술-부채)

---

## 1. 프로젝트 개요

**WatchService Agent**는 실시간 파일시스템 감시와 머신러닝 기반 랜섬웨어 탐지를 결합한 엔드포인트 보안 에이전트다.

| 항목 | 값 |
|------|----|
| **목적** | 랜섬웨어 행위 실시간 탐지 + LLM 기반 대응 가이드 제공 |
| **아키텍처** | 3-서버 분산 시스템 (Spring Boot + FastAPI + React) |
| **데이터베이스** | SQLite (파일 기반, `log.db`) |
| **ML 모델** | XGBoost (binary classifier: benign vs ransomware) |
| **LLM** | Google Gemini 2.5 Flash (대응 가이드 생성) |
| **학습 데이터** | 합성 랜섬웨어 데이터셋 30,000샘플 |

---

## 2. 전체 아키텍처

### 2.1 컴포넌트 구성

```
┌─────────────────────────────────────────────────────────┐
│          React Frontend  (localhost:3000)                │
│  Dashboard · Notifications · Logs · Settings · Admin    │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP REST (CORS)
┌─────────────────────▼───────────────────────────────────┐
│          Spring Boot Backend  (localhost:8080)           │
│                                                         │
│  WatcherService ──► FileCollectorService                │
│       │                    │                            │
│       │            EventWindowAggregator                │
│       │                    │                            │
│       │              AiService ──► GeminiAdviceService  │
│       │                    │                            │
│       │         NotificationService + LogService        │
│       │                    │                            │
│  Controllers (REST API)    │                            │
│  AdminAuthInterceptor ◄────┘                            │
│                                                         │
│  SQLite: log.db                                         │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP REST
┌─────────────────────▼───────────────────────────────────┐
│          FastAPI AI Server  (localhost:8001)             │
│                                                         │
│  POST /api/analyze  →  XGBoost 이진 분류                │
│  POST /predict      →  랜섬웨어 패밀리 분류 (top-k)     │
└─────────────────────────────────────────────────────────┘
                      │ HTTPS
              Google Gemini API
```

### 2.2 포트 및 URL 정리

| 서비스 | 포트 | 주요 역할 |
|--------|------|-----------|
| React Frontend | 3000 | UI |
| Spring Boot | 8080 | 비즈니스 로직 + DB |
| FastAPI AI | 8001 | XGBoost ML 추론 |
| Gemini API | 443 (HTTPS) | LLM 가이드 생성 |

---

## 3. 핵심 데이터 흐름 — 완전 추적

### 3.1 실시간 감시 → AI 분석 → 알림 저장

```
① 사용자가 프론트엔드에서 폴더 선택
   └─ SettingFoldersPage → POST /settings/folders
      └─ SettingsService.addWatchedFolder()
         └─ SettingsRepository → SQLite watched_folder 저장

② WatcherService.startWatching(folderPath)
   └─ java.nio.file.WatchService 등록 (CREATE/MODIFY/DELETE)
   └─ 하위 디렉토리 재귀 등록 (Files.walkFileTree)
   └─ 별도 스레드에서 watch loop 실행

③ 파일 이벤트 발생
   └─ WatcherService: WatchKey.pollEvents()
      └─ 이벤트마다 WatcherEventRecord 생성:
         {ownerKey, eventType, path, eventTimeMs}
      └─ FileCollectorService.analyze(record) 호출

④ FileCollectorService.analyze()
   └─ 현재 파일 상태 수집:
      - Files.size()          → sizeAfter
      - EntropyAnalyzer       → entropyAfter (Shannon entropy)
      - HashCalculator        → SHA-256 해시 (선택적)
      - 확장자 추출           → extAfter
   └─ FileSnapshotStore에서 이전 상태 조회:
      - sizeBefore, entropyBefore, extBefore
   └─ FileAnalysisResult 생성:
      {eventType, path, ownerKey, eventTime,
       sizeBefore, sizeAfter, sizeDiff,
       entropyBefore, entropyAfter, entropyDiff,
       extBefore, extAfter, exists, ...}
   └─ FileSnapshotStore.update() → 현재 상태를 다음 이벤트의 "before"로 저장
   └─ EventWindowAggregator.onFileAnalysisResult() 호출

⑤ EventWindowAggregator (3초 윈도우)
   └─ 이벤트를 currentEvents 리스트에 누적
   └─ 윈도우 시작 후 3초 경과 시 flushWindow() 자동 호출
   └─ flushWindow():
      A. computeWindowStats() → 9개 피처 계산 (아래 §4.4 참조)
      B. AiPayload 빌드
      C. AiService.requestAnalysis(payload) 호출

⑥ AiService.requestAnalysis()
   └─ POST http://localhost:8001/api/analyze
      요청 body: {file_read_count, file_write_count, ...9개 피처}
   └─ AiResponse 수신:
      {status, label, score, detail, topk}
   └─ AiResult.fromResponse() 변환:
      - Benign → SAFE (topProb이 높을수록 riskScore 낮음)
      - 악성 + prob≥0.70 → DANGER
      - 악성 + prob<0.70 → WARNING
   └─ GeminiAdviceService.guidanceFor(payload, aiResult) 호출

⑦ GeminiAdviceService.guidanceFor()
   └─ SAFE: 고정 문구 반환 (Gemini 호출 안 함)
   └─ WARNING/DANGER:
      - buildPrompt() → 간결한 프롬프트 생성
      - callGemini() → POST https://generativelanguage.googleapis.com/...
        {temperature:0.2, maxOutputTokens:400}
      - 응답: [즉시 조치] / [1시간 내] / [오늘 내] / [비고] 섹션
   └─ 실패 시 fallbackGuidance() (하드코딩 SOC 체크리스트) 반환

⑧ 결과 저장 (EventWindowAggregator.flushWindow 계속)
   └─ 윈도우 내 각 FileAnalysisResult에 AiResult 부착
      → LogService.saveAsync(enriched) → LogWriterWorker 큐에 추가
      → 비동기로 SQLite log 테이블에 INSERT
   └─ Notification 엔티티 생성:
      {ownerKey, windowStart, windowEnd, createdAt,
       aiLabel, aiScore, topFamily, aiDetail, guidance,
       affectedFilesCount, affectedPaths(JSON)}
   └─ NotificationService.saveNotification() → SQLite notification 테이블 INSERT

⑨ 프론트엔드 조회
   └─ GET /notifications       → 알림 목록 (페이지네이션)
   └─ GET /notifications/{id}  → 상세 (guidance 포함)
   └─ GET /logs                → 원시 이벤트 로그
   └─ GET /dashboard/summary   → 종합 현황
```

### 3.2 수동 스캔 흐름

```
ScanService.startScan(paths, autoStartWatcher=true)
   └─ ScanJob 생성 (UUID, AtomicInteger counters)
   └─ executor.submit(runScan) → 비동기 실행

runScan():
   └─ countTotalFiles() → 진행률 계산용 총 파일 수
   └─ Files.walk() for each root:
      └─ 각 파일마다:
         - WatcherEventRecord 생성 (eventType="SCAN")
         - FileCollectorService.analyze() → 스냅샷 업데이트
         - SCAN 이벤트는 EventWindowAggregator에서 집계 제외
           (AI 분석·알림 없이 오직 baseline 구축)
   └─ autoStartWatcher=true → WatcherService.startWatchingMultiple() 자동 실행

GET /scan/{scanId}/progress:
   └─ {percent, status(PENDING/RUNNING/DONE/PAUSED/ERROR),
       scanned, total, currentPath}
```

---

## 4. 백엔드 상세 분석

### 4.1 WatcherService

**파일**: `watcher/WatcherService.java`

Java 표준 `java.nio.file.WatchService`를 래핑하여 재귀적 디렉토리 감시를 구현한다.

| 메서드 | 동작 |
|--------|------|
| `startWatching(path)` | 단일 경로 감시 시작 |
| `startWatchingMultiple(paths)` | 복수 경로 동시 감시 |
| `stopWatching()` | 감시 중지, 스레드 인터럽트 |
| `isRunning()` | 현재 감시 상태 조회 |

**내부 동작:**
- `WatchService` 인스턴스를 생성하고 `keyDirMap<WatchKey, Path>`로 관리
- `Files.walkFileTree()`로 전체 하위 디렉토리를 재귀 등록
- `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE` 세 이벤트 타입 감지
- 새 디렉토리 생성 시(`CREATE` 이벤트) 자동으로 WatchKey 추가 등록
- 별도 `watcherThread`에서 `WatchKey.pollEvents()` 루프 실행
- 이벤트 발생 시 `FileCollectorService.analyze()` 직접 호출

**예외 처리:**
- `WatchKey` 무효화(드라이브 제거 등) 시 키 제거 후 경고 로그
- `InterruptedException` 시 스레드 종료 처리

---

### 4.2 FileCollectorService

**파일**: `collector/FileCollectorService.java`

파일 이벤트 하나당 호출되어 파일의 물리적 상태를 측정하고 변화를 계산한다.

**핵심 처리 순서:**

```
1. CREATE/MODIFY:
   - Files.size()        → sizeAfter
   - Files.exists()      → exists=true
   - EntropyAnalyzer.calculate(path) → entropyAfter (Shannon 엔트로피)
   - 확장자 추출(path.getFileName() 파싱)
   - FileSnapshotStore.get(ownerKey, path) → 이전 상태(before) 조회
   - sizeDiff = sizeAfter - sizeBefore
   - entropyDiff = entropyAfter - entropyBefore

2. DELETE:
   - exists=false, sizeAfter=null, entropyAfter=null
   - 이전 상태에서 sizeBefore, entropyBefore 읽기

3. FileAnalysisResult 반환
4. FileSnapshotStore.update() → 현재 상태를 캐시에 저장
```

**EntropyAnalyzer:**
- Shannon 정보 엔트로피: `H = -Σ p(x) * log2(p(x))`
- 최대 1MB 읽기 (`READ_LIMIT = 1_048_576`)
- 바이트별 빈도 계산 후 256-bucket 히스토그램으로 확률 분포 추정
- 암호화된 파일: 엔트로피 ≈ 7.5~8.0 (랜덤에 가까움)
- 일반 텍스트: 엔트로피 ≈ 3~5

**FileSnapshotStore:**
- `ConcurrentHashMap<String, SnapshotEntry>` 인메모리 캐시
- Key: `ownerKey + "|" + path`
- 서버 재시작 시 리셋 (영속성 없음)
- 최대 엔트리 수 제한 (`SnapshotConfig.maxEntries`, 기본 100,000)

---

### 4.3 EventWindowAggregator

**파일**: `analytics/EventWindowAggregator.java`

가장 복잡한 컴포넌트. 3초 시간 윈도우 내 이벤트를 집계하여 9개의 피처를 생성한다.

**윈도우 관리:**
```java
onFileAnalysisResult(result):
  if (eventTime - windowStart >= 3000ms) → flushWindow()
  currentEvents.add(result)
```

**`computeWindowStats()` — 9개 피처 계산 상세:**

| 피처 | 계산 방식 |
|------|-----------|
| `file_read_count` | MODIFY 이벤트 중 "touch 세션" 기준 새 접근 수 (5분 세션 타임아웃) |
| `file_write_count` | MODIFY 이벤트 중 size 또는 entropy 변화가 있는 건수 + rename 수 |
| `file_delete_count` | DELETE 이벤트 건수 (rename으로 판정된 건 제외) |
| `file_rename_count` | DELETE+CREATE 쌍을 점수 매칭으로 탐지 (동일 부모 디렉토리, 시간 gap ≤2초, size 동일+2점, ext 동일+1점, 3점 이상이면 rename) |
| `file_encrypt_like_count` | 엔트로피 증가량 ≥0.30 AND (크기변화 OR 확장자변화) AND 파일크기 ≥4096 bytes |
| `changed_files_count` | 윈도우 내 고유 파일 경로 수 - rename 건수 |
| `random_extension_flag` | 비정상 확장자(화이트리스트 外, 영숫자만, 4자 이상) 파일 ≥2개이면 1, 아니면 0 |
| `entropy_diff_mean` | 엔트로피 변화량 평균 (before/after 쌍이 있는 파일만) |
| `file_size_diff_mean` | 파일 크기 변화량 평균 (byte, 부호 있음) |

**rename 탐지 상세 (`detectRenameLikeCountByScore`):**
```
DELETE 이벤트 목록 × CREATE 이벤트 목록 → 최적 매칭
조건:
  - ownerKey 동일
  - 부모 디렉토리 동일
  - 시간 gap ≤ renameMaxGapMs (기본 2000ms)
점수:
  - size 정확히 동일: +2점
  - size 근사 동일(±1%): +1점
  - 확장자 동일: +1점
3점 이상 → rename으로 간주
```

**화이트리스트 확장자** (랜덤 확장자 플래그에서 제외):
`txt, log, doc, docx, xls, xlsx, pdf, png, jpg, jpeg, gif, zip, rar, 7z`

---

### 4.4 AiService

**파일**: `ai/AiService.java`

FastAPI 서버와의 HTTP 통신을 담당하며 Gemini 호출까지 포함한다.

**`requestAnalysis(payload)` 흐름:**
```
POST /api/analyze → AiResponse
AiResult.fromResponse(response):
  - topFamily = "Benign" → SAFE, riskScore = 1.0 - topProb
  - topFamily = 악성 + topProb ≥ 0.70 → DANGER, riskScore = topProb
  - topFamily = 악성 + topProb < 0.70 → WARNING, riskScore = topProb
  - topFamily = null → UNKNOWN
geminiAdviceService.guidanceFor(payload, result) → guidance 문자열
최종 AiResult에 guidance 포함하여 반환
```

**패밀리 카테고리 분류 (`classifyFamilyCategory`):**

| 카테고리 | 포함 패밀리 |
|----------|-------------|
| BENIGN | Benign |
| RANSOMWARE | Cerber, DarkSide, Dharma, Gandcrab, LockBit, Maze, Phobos, REvil, Ragnar, Ryuk, Shade, WannaCry |
| INFOSTEALER | Raccoon, RedLine, Snake |
| RAT | Agenttesla, Gh0st, NanoCore, njRat, Remcos |
| BOTNET_LOADER | Emotet, Qbot, Ursnif, Glupteba, Guloader, Formbook |
| OTHER | 위 외 모든 경우 |

---

### 4.5 GeminiAdviceService

**파일**: `ai/GeminiAdviceService.java`

Google Gemini API를 호출하여 SOC(보안 관제 센터) 관점의 대응 가이드를 생성한다.

**라벨별 처리:**

| 라벨 | 처리 |
|------|------|
| SAFE | Gemini 미호출, 고정 문구 반환 |
| WARNING | Gemini 호출 (API 키 있을 때) |
| DANGER | Gemini 호출 (API 키 있을 때) |
| API 키 없음 | fallbackGuidance() 반환 |
| 429 에러 | quotaGuidance() 반환 |
| 기타 오류 | fallbackGuidance() 반환 |

**프롬프트 구조 (토큰 최적화 후):**
```
SOC 방어조치 전문가. 수비 관점 조치만 작성(공격정보·코드 제외). 한국어.

탐지: {label} score={score} {detail}
피처: {비-零 피처만 나열 — 예: write=5 del=3 enc=2 entrDiff=0.412}

[즉시 조치]
[1시간 내]
[오늘 내]
[비고] 1~2문장
```

**Gemini 호출 파라미터:**
- 모델: `gemini-1.5-flash` (application.yml 설정값)
- temperature: `0.2`
- maxOutputTokens: `400`
- timeout: `2500ms`

**fallbackGuidance 내용 (하드코딩):**
- [즉시 조치]: 네트워크 분리, 파일 경로 확인, 백업 접근 제한, 로그 보존
- [1시간 내]: EDR 검사, 계정 비밀번호 변경, 횡이동 확인
- [오늘 내]: 백업 무결성 점검, 공유폴더 로그 분석, 보안팀 보고

---

### 4.6 DashboardController

**파일**: `dashboard/DashboardController.java`

`GET /dashboard/summary` 하나의 엔드포인트만 제공.

**응답 구성 로직:**
```
1. AlertService.getAlerts(1, 50, ..., "ALL", ...) → 최근 50개 알림 조회
2. aiLabel별 카운트: dangerCount, warningCount
3. 전체 상태 결정:
   - dangerCount > 0  → "DANGER"
   - warningCount > 0 → "WARNING"
   - 그 외            → "SAFE"
4. SettingsService.getWatchedFolders() → watchedPath (첫 번째 폴더)
5. NotificationService.getLatestNotificationOrNull() → 최신 guidance
6. 응답: {status, statusLabel, lastEventTime, dangerCount, warningCount,
           totalCount, watchedPath, guidance}
```

---

### 4.7 SettingsController — GUI 폴더 선택

**파일**: `settings/SettingsController.java`

`GET /settings/folders/pick` 엔드포인트가 특이하다.

```java
// Java Swing JFileChooser를 백엔드 서버에서 직접 열어
// 사용자가 GUI로 폴더를 선택하게 함
JFileChooser chooser = new JFileChooser();
chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
EventQueue.invokeAndWait(() → {
    int result = chooser.showOpenDialog(null);
    if (result == JFileChooser.APPROVE_OPTION)
        ref.set(chooser.getSelectedFile().getAbsolutePath());
});
```

- `application.yml`에 `java.awt.headless=false` 설정 필수
- headless 환경이면 409 Conflict 반환
- WatchService Agent가 로컬 데스크톱 앱으로 동작하기 때문에 가능한 설계

---

### 4.8 AdminAuthInterceptor

**파일**: `admin/AdminAuthInterceptor.java`

`/api/admin/**` 경로를 모두 보호한다.

```
preHandle():
  URI가 /api/admin/login → 통과
  세션에서 ADMIN_AUTH == Boolean.TRUE → 통과
  그 외 → 401 {"error":"admin_auth_required"} 반환
```

---

### 4.9 SessionIdManager

**파일**: `common/util/SessionIdManager.java`

다중 사용자 격리를 위한 고유 식별자 관리.

- 최초 실행 시 UUID 생성하여 `~/.watchservice/session_id` 파일에 영속 저장
- 이후 재시작 시 파일에서 읽어 동일한 ownerKey 사용
- 파일 I/O 실패 시 임시 UUID 사용 (재시작마다 새 UUID → 기존 데이터 접근 불가)
- 모든 Repository 메서드에서 ownerKey 조건절로 데이터 격리

---

## 5. AI 서버 상세 분석

**파일**: `코드/api_server.py`

### 5.1 모델 로딩 우선순위

```python
artifacts/ 디렉토리에서 순서대로 시도:
1. model_xgb.json   → XGBoost native JSON (save_model 형식)
2. model_lgbm.pkl   → LightGBM pickle
3. model_xgb.pkl    → XGBoost pickle
4. model.pkl        → Sklearn 호환 모델
```

### 5.2 특징 처리

```python
features.json 에 정의된 9개 순서로 입력 정렬
누락된 특징 → -1 로 채움
numpy array → model.predict_proba() 또는 model.predict()
```

### 5.3 `/api/analyze` — 이진 분류 엔드포인트

백엔드 `AiService`가 호출하는 메인 엔드포인트.

**요청:**
```json
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
```

**라벨 결정 로직:**
```python
top_pred = 최고 확률 클래스명
top_prob = 해당 확률

if top_pred == "benign":
    label = "SAFE"
elif top_prob >= 0.70:
    label = "DANGER"
else:
    label = "WARNING"
```

**응답:**
```json
{
  "status": "ok",
  "label": "DANGER",
  "score": 0.87,
  "detail": "top_family=LockBit, top_prob=0.87",
  "message": "Matched 9 features",
  "topk": [
    {"family": "LockBit", "prob": 0.87},
    {"family": "benign", "prob": 0.13}
  ]
}
```

### 5.4 `/predict` — 패밀리 분류 엔드포인트

**요청:**
```json
{
  "features": { ...9개 피처... },
  "topk": 5
}
```

**응답:**
```json
{
  "topk": [
    {"family": "LockBit", "prob": 0.52},
    {"family": "Ryuk", "prob": 0.23},
    ...
  ],
  "message": "Matched 9 features"
}
```

### 5.5 artifacts 파일 구조

```
코드/artifacts/
├── model_xgb.json      # XGBoost 학습 모델 (400트리, ~500KB)
├── features.json       # ["file_read_count", ..., "file_size_diff_mean"]
└── classes.json        # ["benign", "ransomware"]
```

---

## 6. 프론트엔드 상세 분석

### 6.1 라우팅 구조 (App.js)

```
<BrowserRouter>
  <MainLayout>          ← NavSidebar + HeaderBar 포함
    / → MainBoardPage
    /notifications → NotificationPage
    /notifications/stats → NotificationStatsPage
    /notifications/:id → NotificationDetailPage
    /logs → LogsPage
    /notice → UserNoticePage
    /settings → SettingHomePage
    /settings/folders → SettingFoldersPage
    /settings/exceptions → SettingExceptionsPage
    /settings/notify → SettingNotifyPage
    /settings/reset → SettingResetPage
    /settings/update → SettingUpdatePage
    /settings/feedback → SettingFeedbackPage
    /admin/login → AdminLoginPage
    /admin/main → AdminMainPage
    /admin/feedback → AdminFeedbackPage
    /admin/notification → AdminNoticePage
  </MainLayout>
```

### 6.2 API 레이어

**HttpClient.js** — 모든 API 함수의 기반:
```javascript
BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'

get(path)          → fetch(BASE_URL + path)
post(path, body)   → fetch(..., {method:'POST', body:JSON.stringify(body)})
put(path, body)    → fetch(..., {method:'PUT'})
del(path)          → fetch(..., {method:'DELETE'})
```

**페이지네이션 변환 (0-based ↔ 1-based):**
- 프론트엔드 내부: 0-based (page=0이 첫 페이지)
- 백엔드 API: 1-based (page=1이 첫 페이지)
- `NotificationsApi.js`, `LogsApi.js` 모두 `backendPage = page + 1` 변환 적용

### 6.3 커스텀 훅 상세

| 훅 | 관리 상태 | 주요 동작 |
|----|-----------|-----------|
| `UseLogs` | logs, total, page, limit, filters | 로그 목록 + 페이지네이션 + 필터링 |
| `UseNotifications` | notifications, total, page, size | 알림 목록 + 필터링 |
| `UseWatchedFolders` | folders, loading | 폴더 CRUD + 경로에서 이름 추출 |
| `UseExceptions` | items, loading | 예외 규칙 CRUD |
| `UseProtectionStatus` | summary, loading | 대시보드 요약 데이터 |

### 6.4 MainBoardPage 상세

**상태 관리:**
```javascript
summary, summaryLoading, summaryError  → 대시보드 요약
isScanning, scanProgress               → 스캔 상태
pollRef                                → 800ms 폴링 interval ref
```

**스캔 진행률 폴링:**
```javascript
startPolling(scanId):
  pollRef.current = setInterval(async () => {
    const prog = await fetchScanProgress(scanId)
    setScanProgress(prog)
    if (prog.status === 'DONE' || 'PAUSED' || 'ERROR')
      clearInterval(pollRef.current)
  }, 800)
```

**폴더 없을 때 감시 시작 시도:**
```javascript
if (!isWatching):
  try:
    startScan(paths, autoStartWatcher=true) → 성공
  catch:
    startWatcher(folderPath) → 폴백
```

### 6.5 NotificationPage ↔ NotificationDetailPage 간 데이터 전달

```javascript
// NotificationPage에서 상세 페이지로 이동 시
navigate(`/notifications/${item.id}`, {
  state: { notification: item }  // React Router state로 전달
})

// NotificationDetailPage에서
const stateItem = location.state?.notification  // 캐시된 데이터 우선 사용
// stateItem 없으면 fetchAlertDetail(id) API 호출
```

이 패턴으로 목록→상세 이동 시 API 재호출 없이 즉시 렌더링 가능.

### 6.6 LogsPage — 내보내기 구현

```javascript
exportLogs(req) → POST /logs/export
  req: {
    format: 'csv' | 'json',
    ids: [1,2,3] | null,   // null이면 현재 필터 전체
    ...filters
  }

응답 처리:
  CSV → Content-Type: text/csv → Blob 생성 → <a> 태그 다운로드
  JSON → Content-Type: application/json → 파싱 후 처리
```

---

## 7. 데이터베이스 스키마

**엔진**: SQLite 3.46.0.0 (파일: `watchservice_be/log.db`)
**ORM**: 없음 (JdbcTemplate 직접 사용)
**테이블 생성**: `@PostConstruct`로 앱 시작 시 `CREATE TABLE IF NOT EXISTS` 실행

### 7.1 log 테이블 (원시 파일 이벤트 + AI 결과)

```sql
CREATE TABLE IF NOT EXISTS log (
    id                INTEGER  PRIMARY KEY AUTOINCREMENT,
    owner_key         TEXT     NOT NULL,
    event_type        TEXT,                -- CREATE / MODIFY / DELETE / SCAN
    path              TEXT,
    collected_at      INTEGER  NOT NULL,   -- epoch ms
    exists            INTEGER,             -- 0/1 (boolean)
    size              INTEGER,             -- 현재 파일 크기 (bytes)
    exists_before     INTEGER,
    size_before       INTEGER,
    size_after        INTEGER,
    size_diff         INTEGER,
    entropy_before    REAL,                -- Shannon 엔트로피
    entropy_after     REAL,
    entropy_diff      REAL,
    ext_before        TEXT,                -- 확장자 (점 제외)
    ext_after         TEXT,
    last_modified_time TEXT,
    hash              TEXT,                -- SHA-256 (현재 미사용, NULL)
    entropy           REAL,                -- 레거시 필드
    ai_label          TEXT,                -- SAFE / WARNING / DANGER / UNKNOWN
    ai_score          REAL,                -- 위험도 0~1
    ai_detail         TEXT                 -- "top_family=LockBit, top_prob=0.87"
);
```

### 7.2 notification 테이블 (3초 윈도우 단위 AI 분석 결과)

```sql
CREATE TABLE IF NOT EXISTS notification (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_key            TEXT    NOT NULL,
    window_start         INTEGER NOT NULL,   -- epoch ms
    window_end           INTEGER NOT NULL,   -- epoch ms
    created_at           INTEGER NOT NULL,   -- epoch ms
    ai_label             TEXT,               -- SAFE / WARNING / DANGER / UNKNOWN
    ai_score             REAL,
    top_family           TEXT,               -- Benign / LockBit / ...
    ai_detail            TEXT,
    guidance             TEXT,               -- Gemini 생성 또는 fallback 안내문
    affected_files_count INTEGER NOT NULL,
    affected_paths       TEXT    NOT NULL    -- JSON 배열: ["path1","path2",...]
);
```

### 7.3 watched_folder 테이블 (모니터링 대상 폴더)

```sql
CREATE TABLE IF NOT EXISTS watched_folder (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_key  TEXT    NOT NULL,
    name       TEXT    NOT NULL,
    path       TEXT    NOT NULL,
    created_at INTEGER NOT NULL
);
```

### 7.4 exception_rule 테이블 (제외 패턴)

```sql
CREATE TABLE IF NOT EXISTS exception_rule (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_key  TEXT    NOT NULL,
    type       TEXT    NOT NULL,   -- PATH / EXT
    pattern    TEXT    NOT NULL,
    memo       TEXT,
    created_at INTEGER NOT NULL
);
```

### 7.5 feedback 테이블 (사용자 피드백)

```sql
CREATE TABLE IF NOT EXISTS feedback (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    email      TEXT,
    content    TEXT    NOT NULL,
    created_at INTEGER NOT NULL
);
```

### 7.6 notice 테이블 (시스템 공지사항)

```sql
CREATE TABLE IF NOT EXISTS notice (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    title      TEXT    NOT NULL,
    content    TEXT    NOT NULL,
    created_at INTEGER NOT NULL
);
```

### 7.7 feedback_ticket 테이블 (레거시, support 패키지용)

```sql
-- 현재 프론트엔드에서 미사용 (support 패키지 삭제됨)
CREATE TABLE IF NOT EXISTS feedback_ticket (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_key  TEXT    NOT NULL,
    name       TEXT,
    email      TEXT,
    content    TEXT    NOT NULL,
    created_at INTEGER NOT NULL
);
```

---

## 8. 전체 API 명세

### 8.1 Dashboard

| 메서드 | 경로 | 설명 | 응답 |
|--------|------|------|------|
| GET | `/dashboard/summary` | 종합 현황 | `{status, statusLabel, lastEventTime, dangerCount, warningCount, totalCount, watchedPath, guidance}` |

### 8.2 Watcher

| 메서드 | 경로 | 파라미터 | 응답 |
|--------|------|----------|------|
| POST/GET | `/watcher/start` | `?folderPath=<path>` | `{message, path}` |
| POST/GET | `/watcher/stop` | — | `{message}` |
| GET | `/watcher/status` | — | `{running, path}` |

### 8.3 Logs

| 메서드 | 경로 | 파라미터/Body | 응답 |
|--------|------|---------------|------|
| GET | `/logs/recent` | `?limit=50` | `List<LogResponse>` |
| GET | `/logs` | `?page&size&from&to&keyword&aiLabel&eventType&sort` | `LogPageResponse` |
| GET | `/logs/{id}` | — | `LogResponse` |
| DELETE | `/logs/{id}` | — | 204 |
| POST | `/logs/delete` | `{ids:[1,2,3]}` | `{deletedCount}` |
| POST | `/logs/export` | `{format,ids,from,to,keyword,...}` | CSV 또는 JSON |

**LogResponse 필드:**
```json
{
  "id": 1,
  "ownerKey": "uuid",
  "eventType": "MODIFY",
  "path": "/Users/test/doc.txt",
  "collectedAt": "2026-03-05 14:30:00",
  "exists": true,
  "size": 4096,
  "sizeBefore": 3000, "sizeAfter": 4096, "sizeDiff": 1096,
  "entropyBefore": 4.2, "entropyAfter": 7.8, "entropyDiff": 3.6,
  "extBefore": "txt", "extAfter": "enc",
  "hash": null,
  "aiLabel": "DANGER",
  "aiScore": 0.87,
  "aiDetail": "top_family=LockBit, top_prob=0.87"
}
```

### 8.4 Notifications (3초 윈도우 AI 결과)

| 메서드 | 경로 | 파라미터 | 응답 |
|--------|------|----------|------|
| GET | `/notifications` | `?page&size&from&to&level&keyword&sort` | `NotificationPageResponse` |
| GET | `/notifications/{id}` | — | `NotificationResponse` |

**NotificationResponse 필드:**
```json
{
  "id": 42,
  "windowStart": "2026-03-05 14:30:00",
  "windowEnd": "2026-03-05 14:30:03",
  "createdAt": "2026-03-05 14:30:03",
  "aiLabel": "DANGER",
  "aiScore": 0.87,
  "topFamily": "LockBit",
  "aiDetail": "top_family=LockBit, top_prob=0.87",
  "guidance": "[즉시 조치]\n- 네트워크 분리...\n[1시간 내]...",
  "affectedFilesCount": 15,
  "affectedPaths": ["/Users/test/doc1.txt", "/Users/test/doc2.pdf"]
}
```

### 8.5 Alerts (로그 기반 AI 결과 뷰)

| 메서드 | 경로 | 파라미터 | 응답 |
|--------|------|----------|------|
| GET | `/alerts` | `?page&size&from&to&level&keyword&sort` | `AlertPageResponse` |
| GET | `/alerts/{id}` | — | `LogResponse` |
| GET | `/alerts/stats` | `?range=daily&from&to` | `AlertStatsResponse` |

**AlertStatsResponse:**
```json
{
  "range": "daily",
  "series": [
    {"date": "2026-03-05", "dangerCount": 3, "warningCount": 7}
  ]
}
```

### 8.6 Settings

| 메서드 | 경로 | Body | 응답 |
|--------|------|------|------|
| GET | `/settings/folders` | — | `List<WatchedFolderResponse>` |
| POST | `/settings/folders` | `{name, path}` | `WatchedFolderResponse` |
| DELETE | `/settings/folders/{id}` | — | 204 |
| GET | `/settings/folders/pick` | — | `{path}` 또는 409 |
| GET | `/settings/exceptions` | — | `List<ExceptionRuleResponse>` |
| POST | `/settings/exceptions` | `{type, pattern, memo}` | `ExceptionRuleResponse` |
| DELETE | `/settings/exceptions/{id}` | — | 204 |

### 8.7 Scan

| 메서드 | 경로 | Body/파라미터 | 응답 |
|--------|------|---------------|------|
| POST | `/scan/start` | `{paths:[...], autoStartWatcher:true}` | `{scanId}` |
| POST | `/scan/{scanId}/pause` | — | `ScanProgressResponse` |
| GET | `/scan/{scanId}/progress` | — | `{percent, status, scanned, total, currentPath}` |

### 8.8 Admin (세션 인증 필요)

| 메서드 | 경로 | Body | 응답 |
|--------|------|------|------|
| POST | `/api/admin/login` | `{username, password}` | `{result:"ok"}` 또는 401 |
| POST | `/api/admin/logout` | — | `{result:"ok"}` |
| GET | `/api/admin/feedback` | — | `List<FeedbackDto>` |
| DELETE | `/api/admin/feedback/{id}` | — | 204 |
| POST | `/api/admin/notifications` | `{title, content}` | 200 |
| DELETE | `/api/admin/notifications/{id}` | — | 204 |

### 8.9 Public (인증 불필요)

| 메서드 | 경로 | 응답 |
|--------|------|------|
| GET | `/api/notifications` | `List<NoticeDto>` (공지사항) |
| POST | `/api/feedback` | 200 또는 400 |

### 8.10 AI 직접 테스트용

| 메서드 | 경로 | 응답 |
|--------|------|------|
| POST | `/ai/analyze` | `AiResult` |
| POST | `/ai/family/predict` | `FamilyPredictResponse` |
| GET | `/ai/ping` | health check |

---

## 9. 보안 · 인증 구조

### 9.1 관리자 인증

```
흐름:
POST /api/admin/login {username, password}
  → AdminAuthService.authenticate() → 단순 문자열 비교
  → 성공: session.setAttribute("ADMIN_AUTH", Boolean.TRUE)
  → 실패: 401 {error: invalid_credentials}

이후 /api/admin/** 요청:
  → AdminAuthInterceptor.preHandle()
  → session.getAttribute("ADMIN_AUTH") != Boolean.TRUE → 401
```

**자격증명 관리:**
- `application.yml`: `admin.username`, `admin.password`
- 환경변수 오버라이드: `ADMIN_USERNAME`, `ADMIN_PASSWORD`
- **현재: 평문 저장, 해시 없음**

### 9.2 사용자 격리 (Multi-tenancy)

- `SessionIdManager`가 UUID를 `~/.watchservice/session_id`에 영속
- 모든 DB 쿼리에 `WHERE owner_key = ?` 조건 포함
- 사용자 A의 로그는 사용자 B가 접근 불가

### 9.3 CORS 설정

```java
// WebConfig.java + 각 Controller @CrossOrigin
허용 Origin: http://localhost:3000, http://localhost:5173
허용 Methods: GET, POST, PUT, DELETE, OPTIONS
허용 Headers: 모든 헤더
allowCredentials: true
```

### 9.4 Gemini API Key 보안

- `application.yml`: `gemini.api-key: ${GEMINI_API_KEY:}`
- 환경변수에서만 주입 (코드에 하드코딩 없음)
- 프론트엔드에 절대 노출되지 않음

---

## 10. 설정값 레퍼런스

**파일**: `watchservice_be/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:sqlite:log.db
    driver-class-name: org.sqlite.JDBC

server:
  port: 8080

ai:
  analyze:
    url: http://localhost:8001/api/analyze  # FastAPI 이진 분류
  family:
    url: http://localhost:8001/predict      # FastAPI 패밀리 분류

gemini:
  api-key: ${GEMINI_API_KEY:}              # 환경변수로 주입
  model: gemini-1.5-flash
  timeout-ms: 2500

admin:
  username: ${ADMIN_USERNAME:admin}
  password: ${ADMIN_PASSWORD:123456789}

watchservice:
  analytics:
    window-ms: 3000                        # 3초 집계 윈도우
    touch-session-timeout-ms: 300000       # 5분 touch 세션
    rename-max-gap-ms: 2000               # rename 쌍 허용 시간 간격
    encrypt:
      entropy-diff-threshold: 0.30        # 암호화 의심 엔트로피 변화 임계값
      min-size-bytes: 4096               # 최소 파일 크기 (4KB)
      eps: 1.0E-6                        # 부동소수점 비교 epsilon
    random-ext:
      min-count: 2                        # 비정상 확장자 최소 파일 수
      min-length: 4                       # 비정상 확장자 최소 길이
      whitelist: "txt,log,doc,docx,xls,xlsx,pdf,png,jpg,jpeg,gif,zip,rar,7z"
```

---

## 11. ML 모델 학습 파이프라인

### 11.1 학습 데이터

| 데이터소스 | 설명 |
|-----------|------|
| `synthetic_ransomware_dataset_30000.csv` | 합성 데이터 30,000샘플 (benign/ransomware 각 15,000) |
| CAPE 샌드박스 리포트 | PE 섹션 엔트로피 피처 추출 (`cape_ransap_entropy.py`) |
| Mordor 보안 이벤트 | OTRF GitHub에서 Sysmon 이벤트 로그 다운로드 (`mordor_ransap_features.csv.py`) |

### 11.2 XGBoost 학습 (`untitled21.py`)

```python
XGBClassifier(
    n_estimators=400,        # 트리 수
    learning_rate=0.05,      # 학습률
    max_depth=4,             # 과적합 방지
    subsample=0.8,           # 행 샘플링
    colsample_bytree=0.8,    # 열 샘플링
    objective='binary:logistic',
    eval_metric='logloss',
    tree_method='hist'       # GPU: gpu_hist
)
```

**학습 결과 평가:**
- Classification report (precision, recall, F1)
- Confusion matrix
- ROC-AUC 점수

**저장:**
```python
clf.save_model('artifacts/model_xgb.json')  # XGBoost native 형식
```

### 11.3 피처 엔지니어링 (Mordor 데이터)

```python
Sysmon EventID 매핑:
  1  → Process Create
  11 → File Write/Create
  12 → Registry Modification
  13 → File Rename

추출 피처:
  file_write_count       EventID=11 건수
  file_rename_count      EventID=13 건수
  process_spawn_count    EventID=1  건수
  registry_modify_count  EventID=12 건수
  entropy_diff_mean      파일 엔트로피 변화 평균
  file_size_diff_mean    파일 크기 변화 평균
```

---

## 12. 알려진 이슈 및 기술 부채

### 12.1 기능적 이슈

| 이슈 | 위치 | 심각도 |
|------|------|--------|
| `hash` 필드 항상 NULL | `Log.java`, LogWriterWorker | 낮음 (기능 무관) |
| `NotificationStatsPage`가 항상 빈 데이터 표시 | `NotificationsApi.fetchAlertStats()` → 목업 반환 | 중간 |
| `SettingUpdatePage` 실제 업데이트 미구현 | 버튼 클릭 시 alert()만 호출 | 낮음 |
| `feedback_ticket` 테이블 프론트에서 미사용 | SupportRepository | 낮음 |

### 12.2 보안 이슈

| 이슈 | 설명 | 권고 |
|------|------|------|
| 관리자 비밀번호 평문 저장 | `AdminAuthService` 단순 문자열 비교 | BCrypt 해싱 적용 |
| SessionIdManager I/O 실패 시 임시 UUID | 재시작마다 새 UUID → 기존 데이터 접근 불가 | 실패 시 예외 처리 또는 영속 fallback |
| `java.awt.headless=false` | GUI 폴더 선택을 위해 전역 설정 | 서버 배포 환경 부적합 |

### 12.3 코드 품질

| 이슈 | 위치 |
|------|------|
| `parseFromToEpochStart/End` 동일 로직 3개 서비스에 중복 | AlertService, NotificationService, LogService |
| `normalizeLevel()` 동일 로직 2개 서비스에 중복 | AlertService, NotificationService |
| `SortInfo` 이너 클래스 3개 서비스에 중복 | 위와 동일 |
| `toQuery()` 유틸 함수 2개 API 파일에 중복 | LogsApi.js, NotificationsApi.js |
| LogService `parseFromToEpochEnd()` → `d.plusDays(1)` 사용 | AlertService/NotificationService는 `23:59:59.999` 방식으로 불일치 |

### 12.4 성능 고려사항

| 항목 | 현황 |
|------|------|
| `FileSnapshotStore` | 인메모리 캐시, 서버 재시작 시 초기화 → 첫 이벤트에 before 값 없음 |
| 비동기 로그 저장 | `LogService.saveAsync()` → 큐 기반, DB 쓰기 블로킹 없음 |
| SQLite 단일 쓰기 | 동시 쓰기 시 WAL 모드 미설정 → 부하 시 잠금 경합 가능 |
| Gemini 타임아웃 | 2500ms, 초과 시 fallbackGuidance 반환 |
| AI 서버 타임아웃 | RestTemplate 기본값 (무한), AI 서버 다운 시 블로킹 위험 |

---

*보고서 끝*
