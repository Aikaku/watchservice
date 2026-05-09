# WatchService Agent — 랜섬웨어 실시간 탐지 시스템

파일시스템을 실시간으로 감시하여 랜섬웨어 행위 패턴을 탐지하고, 경보와 대응 가이드를 제공하는 데스크탑 보안 에이전트다.

[![다운로드](https://img.shields.io/badge/다운로드-v1.0.0-blue)](https://aikaku.github.io/watchservice/download_site/)
[![GitHub](https://img.shields.io/badge/GitHub-Aikaku%2Fwatchservice-181717?logo=github)](https://github.com/Aikaku/watchservice)
[![릴리스](https://img.shields.io/github/v/release/Aikaku/watchservice)](https://github.com/Aikaku/watchservice/releases)

---

## 시스템 구성

```
┌─────────────────────────────────────────────────────────────┐
│  Electron 데스크탑 앱 (watchservice_electron)                │
│  - Spring Boot JAR를 내부에서 자동 실행                      │
│  - 시스템 트레이 상주                                        │
└────────────────────────┬────────────────────────────────────┘
                         │ 내장 브라우저 (localhost:8080)
┌────────────────────────▼────────────────────────────────────┐
│  watchservice_be  (Spring Boot, port 8080)                  │
│  - React 정적 파일 서빙 (watchservice_fe 빌드 결과)          │
│  - NIO WatchService로 폴더 재귀 감시                         │
│  - 3초 윈도우 단위 피처 집계                                  │
│  - SQLite 로그 저장 (log.db)                                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP (기본: localhost:8001)
┌────────────────────────▼────────────────────────────────────┐
│  AI 서버  (FastAPI + XGBoost, port 8001)                    │
│  - 9개 피처 → 랜섬웨어 판정 (SAFE / WARNING / DANGER)        │
│  - 랜섬웨어 패밀리 분류                                       │
└─────────────────────────────────────────────────────────────┘
                         │
              Gemini LLM API (선택)
              대응 가이드 생성
```

> **개발 환경**: `npm start`(포트 3000) + `./gradlew bootRun`(포트 8080) 을 분리 실행 가능.
> **배포 환경**: `build.sh --electron`으로 Electron `.dmg` / `.msi` 인스톨러 생성.

---

## 요구 사항

| 구성 요소 | 버전 |
|-----------|------|
| Java | 17 |
| Gradle | 8.x |
| Node.js | 18+ |
| Python | 3.9+ |
| Python 패키지 | `fastapi`, `uvicorn`, `xgboost`, `lightgbm`, `pandas`, `numpy`, `pyyaml` |

---

## 빠른 시작

### 방법 A — 데스크탑 앱 (인스톨러)

**[다운로드 페이지](https://aikaku.github.io/watchservice/download_site/)** 에서 `.dmg` (macOS) 또는 `.msi` (Windows) 설치 후 실행.
Java 설치 불필요 — JRE가 앱 안에 번들되어 있다.

> AI 서버는 별도 배포 필요 (아래 [AI 서버 실행](#1-ai-서버-실행) 참고).

---

### 방법 B — 소스에서 직접 실행 (개발)

#### 1. AI 서버 실행

```bash
cd 코드/
pip install -r ../ai_server_deploy/requirements.txt
python api_server.py
# → http://localhost:8001
```

아티팩트 파일(`model_xgb.json`, `features.json`, `classes.json`)은 `코드/artifacts/` 폴더에 위치해야 한다.

#### 2. 백엔드 실행

```bash
cd watchservice_be/

# .env 파일 생성 (최초 1회)
cp .env.example .env   # 없으면 아래 환경변수를 직접 설정

./gradlew bootRun
# → http://localhost:8080
```

#### 3. 프론트엔드 실행 (개발 서버)

```bash
cd watchservice_fe/
npm install
npm start
# → http://localhost:3000  (개발 시에만 분리 실행)
```

---

### 방법 C — 통합 빌드 (Electron 인스톨러 생성)

```bash
# macOS — .dmg 생성
./build.sh --electron

# Windows — .msi 생성
build.bat --electron
```

빌드 결과: `watchservice_electron/dist/` 에 인스톨러 파일 생성.

> `--electron` 없이 실행하면 JAR 파일까지만 빌드하고 종료된다.

---

## 환경변수 설정 (`.env`)

`watchservice_be/` 루트에 `.env` 파일을 생성한다.

```env
# 관리자 계정
ADMIN_USERNAME=your-admin-username
ADMIN_PASSWORD=$2a$10$...   # BCrypt 해시 권장 (평문도 가능)

# Gemini API (선택 — 없으면 대응 가이드 생략)
GEMINI_API_KEY=AIza...

# AI 서버 주소 (기본값: localhost:8001)
AI_ANALYZE_URL=http://localhost:8001/api/analyze
AI_FAMILY_URL=http://localhost:8001/predict

# AI 서버 타임아웃 (선택)
AI_CONNECT_TIMEOUT_MS=5000
AI_READ_TIMEOUT_MS=15000

# CORS (운영 배포 시 실제 도메인으로 변경)
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**BCrypt 해시 생성:**
```bash
# CLI (htpasswd 사용)
htpasswd -bnBC 10 "" yourPassword | tr -d ':\n'
```

---

## 주요 기능

### 탐지 파이프라인

```
파일 이벤트 (CREATE / MODIFY / DELETE)
  → 엔트로피·크기·확장자 분석
  → 3초 윈도우 집계 (9개 피처)
  → XGBoost 모델 예측
  → SAFE / WARNING / DANGER 판정
  → 알림 저장 + Gemini 대응 가이드 생성
```

### 추가 구현 기능

| 기능 | 설명 |
|------|------|
| 이메일 알림 | DANGER 탐지 시 경고 메일 자동 발송 |
| 수동 스캔 | 원하는 시점에 감시 폴더 전수 스캔 |
| 오탐 신고 | 알림에서 원클릭으로 피드백·예외 규칙 등록 |
| 긴급 전체화면 경보 | DANGER 탐지 시 Electron 전체화면 경보 강제 표시 |
| 탐지 리포트 PDF | 기간별 탐지 현황 PDF 내보내기 |
| 심각도별 소리 알림 | 심각도에 따른 차별화 경고음 |
| 감시 스케줄 설정 | 특정 시간대·요일에만 감시 활성화 |
| 랜섬웨어 패밀리 상세 정보 | 패밀리별 공격 방식·대응 방법 안내 |
| 파일 확장자 분포 차트 | 탐지 이벤트 확장자 분포 시각화 |
| 자주 변경 파일 Top N | 집중 공격 대상 파일 순위 차트 |
| 파일 권한 보안 감사 | others 쓰기·실행 권한 파일 조회 (macOS/Linux) |
| 컴퓨터 시작 시 자동 실행 | 설정 페이지에서 시작 프로그램 등록·해제 |

### 9개 피처 (AiPayload)

| 피처 | 설명 |
|------|------|
| `fileReadCount` | 파일 접근 수 추정 (MODIFY + DELETE 기반 touch session) |
| `fileWriteCount` | 파일 쓰기 수 (MODIFY 내용 변화 + 대용량 CREATE) |
| `fileDeleteCount` | 파일 삭제 수 (rename 보정 후) |
| `fileRenameCount` | 파일 rename 수 (DELETE+CREATE 점수 매칭) |
| `fileEncryptLikeCount` | 암호화 의심 파일 수 (엔트로피↑, 고엔트로피 CREATE, 스트림 암호 패턴) |
| `changedFilesCount` | 변경된 고유 파일 수 |
| `randomExtensionFlag` | suspicious 확장자 집단 탐지 (0 또는 1) |
| `entropyDiffMean` | 유의미한 엔트로피 변화 평균 (\|diff\| > 0.05) |
| `fileSizeDiffMean` | 파일 크기 변화 평균 (bytes) |

### 화면 구성

| 경로 | 설명 |
|------|------|
| `/` | 메인 대시보드 — 실시간 감시 상태, 최근 알림 |
| `/notifications` | 탐지 알림 목록 |
| `/notifications/stats` | 알림 통계 차트 (SAFE / WARNING / DANGER 분포) |
| `/logs` | 이벤트 로그 조회·CSV 내보내기 |
| `/settings/**` | 감시 폴더·예외 규칙·알림 설정 |
| `/admin/**` | 관리자 전용 페이지 (로그인 필요) |

---

## 관리자 페이지

`/admin/login` 에서 로그인 후 사용 가능.
기본 계정: `admin` / `123456789` (운영 전 반드시 변경).

| 메뉴 | 기능 |
|------|------|
| 세션 관리 | 현재 접속 ownerKey 목록·강제 종료 |
| 알림 관리 | 전체 알림 조회·삭제 |
| 로그 관리 | 이벤트 로그 전체 조회·삭제 |
| 피드백 관리 | 사용자 문의·피드백 확인·삭제 |
| 공지사항 | 사용자 대상 공지 작성·관리 |
| 시스템 정보 | 서버 상태·설정값 확인 |

---

## 프로젝트 구조

```
통합/
├── watchservice_be/          # Spring Boot 백엔드 (에이전트)
│   ├── src/main/java/.../
│   │   ├── watcher/          # NIO WatchService 파일 감시
│   │   ├── collector/        # 파일 분석 (엔트로피·크기·확장자)
│   │   ├── analytics/        # 3초 윈도우 집계·AI 호출
│   │   ├── ai/               # XGBoost 서버 통신·Gemini 가이드
│   │   ├── storage/          # SQLite 로그 저장
│   │   ├── alerts/           # 알림 저장·조회
│   │   ├── settings/         # 감시 폴더·예외 규칙 설정
│   │   └── admin/            # 관리자 인증·관리 기능
│   └── src/main/resources/
│       ├── application.yml
│       └── logback-spring.xml
├── watchservice_fe/          # React 프론트엔드
│   └── src/
│       ├── api/              # HTTP 클라이언트·도메인별 API 래퍼
│       ├── components/       # 공통 컴포넌트 (Toast, Modal 등)
│       ├── layout/           # MainLayout, NavSidebar
│       └── pages/            # 화면별 컴포넌트
├── watchservice_electron/    # Electron 데스크탑 앱 래퍼
│   ├── main.js               # Spring Boot 프로세스 관리·트레이
│   ├── preload.js
│   ├── package.json          # electron-builder 설정 (.dmg/.msi)
│   └── assets/               # 아이콘·로딩 화면·오류 화면
├── 코드/                     # AI 서버 (FastAPI + XGBoost)
│   ├── api_server.py         # FastAPI 엔드포인트
│   └── artifacts/            # 학습된 모델·피처 목록
│       ├── model_xgb.json
│       ├── features.json
│       └── classes.json
├── ai_server_deploy/         # AI 서버 클라우드 배포 패키지
│   ├── requirements.txt
│   ├── Procfile              # Railway/Render 시작 명령
│   ├── railway.toml
│   └── README_DEPLOY.md      # 배포 절차 가이드
├── download_site/            # GitHub Pages 다운로드 페이지
│   ├── index.html
│   └── INSTALL_GUIDE.md
├── .github/workflows/
│   ├── release.yml           # 태그 푸시 → macOS/Windows 자동 빌드 + 릴리스
│   └── pages.yml             # download_site → GitHub Pages 자동 배포
├── build.sh                  # 통합 빌드 스크립트 (macOS/Linux)
├── build.bat                 # 통합 빌드 스크립트 (Windows)
├── jre_build.sh              # jlink 최소 JRE 생성 (macOS/Linux)
└── jre_build.bat             # jlink 최소 JRE 생성 (Windows)
```

---

## API 엔드포인트 (주요)

### 백엔드 (`:8080`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/watcher/status` | 감시 상태 조회 |
| `POST` | `/api/watcher/start` | 감시 시작 |
| `POST` | `/api/watcher/stop` | 감시 중지 |
| `GET` | `/api/notifications` | 알림 목록 |
| `GET` | `/api/notifications/stats` | 알림 통계 |
| `GET` | `/api/logs` | 이벤트 로그 조회 |
| `GET` | `/api/settings` | 설정 조회 |
| `POST` | `/api/feedback` | 피드백 제출 |
| `GET` | `/actuator/health` | 헬스체크 |

### AI 서버 (`:8001`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/analyze` | 9개 피처 → SAFE/WARNING/DANGER 판정 |
| `POST` | `/predict` | 랜섬웨어 패밀리 분류 |
| `GET` | `/health` | AI 서버 상태·모델 로드 여부 |

---

## 데이터베이스

SQLite (`log.db`) — 프로젝트 루트에 자동 생성.

| 테이블 | 내용 |
|--------|------|
| `event_log` | 파일 이벤트 로그 |
| `notification` | 탐지 알림 (SAFE/WARNING/DANGER) |
| `settings` | 감시 폴더·예외 규칙 |
| `feedback` | 사용자 피드백 |
| `notice` | 공지사항 |

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [유스케이스.md](문서/유스케이스.md) | 사용자 유스케이스 29개 상세 정의 (액터·흐름·예외 포함) |
| [구현예정_기능명세.md](문서/구현예정_기능명세.md) | 추가 구현 기능 상세 명세 (동작 흐름·API·DB 설계) |
| [추가기능_후보.md](문서/추가기능_후보.md) | 추가 구현 가능 기능 후보 59개 (임팩트·난이도·구현 방법) |
| [진행상황표.md](문서/진행상황표.md) | 주별 진행 상황 (보안·백엔드·프론트·AI·배포·추가기능 포함) |
| [인수테스트_시나리오.md](문서/인수테스트_시나리오.md) | 사용자 인수테스트 시나리오 13개 (설치부터 종료까지) |
| [테스트_가이드.md](문서/테스트_가이드.md) | 기능 테스트 체크리스트 및 단계별 테스트 가이드 |
| [배포_가이드.md](문서/배포_가이드.md) | Electron 인스톨러·AI 서버 배포 절차 |
| [FEATURE_CHANGES.md](FEATURE_CHANGES.md) | AI 피처 계산 방식 변경 이력 |
| [ai_server_deploy/README_DEPLOY.md](ai_server_deploy/README_DEPLOY.md) | AI 서버 Railway/Render 배포 절차 |
| [download_site/INSTALL_GUIDE.md](download_site/INSTALL_GUIDE.md) | Windows/macOS 설치 가이드 |
| [CLAUDE.md](CLAUDE.md) | 개발 가이드 (Claude Code용) |
