# Electron 데스크탑 앱 배포 계획서

> 작성일: 2026-03-23
> 목표: 랜섬웨어 실시간 탐지 시스템을 Windows / macOS 데스크탑 앱으로 배포

---

## 1. 목표

| 항목 | 내용 |
|------|------|
| 배포 형태 | Windows `.msi` / macOS `.dmg` 인스톨러 |
| 사용 방식 | 설치 후 백그라운드에서 자기 PC 파일 감시 |
| 서버 불필요 | 각자 PC에서 독립 실행 (AI 서버만 클라우드) |
| 사용자 인증 | 불필요 (1인 1설치 방식) |

---

## 2. 최종 아키텍처

```
[사용자 PC — Electron 앱]
│
├── Electron Shell
│     └── Electron 창 → React UI 표시
│
├── Spring Boot JAR (백그라운드 자동 실행)
│     ├── WatcherService    → 로컬 파일 감시
│     ├── FileCollector     → 엔트로피·크기·확장자 분석
│     ├── EventWindowAgg    → 3초 윈도우 피처 계산
│     └── SQLite (log.db)   → 로컬 DB
│
└── JRE (번들 포함 — 사용자 Java 설치 불필요)

[클라우드 — AI 서버]
└── Python FastAPI (XGBoost 탐지 + Gemini 가이드)
      ← Spring Boot가 HTTP로 호출
```

---

## 3. 현재 구조에서 변경되는 것

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 실행 방식 | 터미널에서 수동 실행 | 앱 아이콘 클릭으로 실행 |
| React 서빙 | 포트 3000 별도 실행 | Spring Boot가 정적 파일 서빙 (단일 포트 8080) |
| AI 서버 | 로컬 localhost:8001 | 클라우드 URL로 변경 |
| 패키지 | JAR + npm | 단일 인스톨러 (.msi / .dmg) |

---

## 4. 단계별 작업 계획

### Phase 1 — React + Spring Boot 단일 포트 통합

**목표**: React 빌드 결과물을 Spring Boot가 직접 서빙하여 포트를 8080 하나로 통합

**작업**:
- `npm run build` 결과물을 `watchservice_be/src/main/resources/static/`에 복사
- Spring Boot `WebConfig`에서 SPA 라우팅 처리 (모든 경로 → `index.html`)
- React의 `REACT_APP_API_BASE_URL` 제거 (같은 포트에서 서빙되므로 불필요)
- 통합 빌드 스크립트 작성 (`build.sh` / `build.bat`)

**완료 기준**: 브라우저에서 `localhost:8080` 접속 시 React 앱이 정상 동작

---

### Phase 2 — Electron Shell 구성

**목표**: Electron이 Spring Boot를 자식 프로세스로 실행하고, 창에 `localhost:8080`을 표시

**작업**:
- Electron 프로젝트 초기화 (`watchservice_electron/`)
- `main.js`: Spring Boot JAR를 `child_process.spawn()`으로 실행
- Spring Boot 준비 완료 감지 (포트 8080 열릴 때까지 대기 후 창 표시)
- 앱 종료 시 Spring Boot 프로세스 함께 종료
- 앱 아이콘, 창 크기, 타이틀 설정
- 시스템 트레이 아이콘 등록 (백그라운드 감시 중 표시)

**완료 기준**: Electron 앱 실행 시 대시보드 창이 자동으로 열림

---

### Phase 3 — AI 서버 클라우드 배포

**목표**: `api_server.py`를 클라우드 서버에 배포하여 외부 URL로 호출 가능하게 함

**작업**:
- `requirements.txt` 작성 (현재 미존재)
- `configs/config.yaml` 작성 또는 환경변수 기반으로 전환
- 클라우드 플랫폼 선택 및 배포 (Railway / Render / AWS EC2 중 택1)
- Spring Boot `application.yml`의 AI 서버 URL을 클라우드 주소로 변경
- 환경변수 `AI_SERVER_URL`로 주입 가능하도록 수정

**완료 기준**: 로컬 Python 서버 없이 탐지 기능이 정상 동작

---

### Phase 4 — JRE 번들링 및 인스톨러 빌드

**목표**: 사용자가 Java 설치 없이 앱을 실행할 수 있도록 JRE를 포함한 인스톨러 생성

**작업**:
- `jdeps` / `jlink`로 최소 JRE 생성 (전체 JRE 대비 용량 절감)
- `electron-builder` 설정
  - Windows: `.msi` 타겟
  - macOS: `.dmg` 타겟
- 인스톨러에 JRE + Spring Boot JAR 포함
- Windows 시작 프로그램 자동 등록 옵션 추가
- macOS Launch Agent 등록 옵션 추가

**완료 기준**: 클린 환경(Java 미설치)에서 인스톨러 실행 후 앱 정상 동작

---

### Phase 5 — 배포 사이트 구성

**목표**: 사용자가 인스톨러를 다운로드할 수 있는 웹 페이지 구성

**작업**:
- 다운로드 페이지 제작 (Windows / macOS 버튼 구분)
- 설치 가이드 작성
- 버전 관리 (GitHub Releases 활용)

**완료 기준**: 웹 페이지에서 OS별 인스톨러 다운로드 가능

---

### Phase 6 — 코드 서명 ❌ 제외

> 유료 (Windows EV 인증서 $200~$500/년, macOS Apple Developer $99/년) — 비용 문제로 제외.
> 설치 시 OS 보안 경고가 표시되지만 "그래도 열기"로 정상 설치 가능.

---

## 5. 기술 스택

| 구분 | 기술 |
|------|------|
| 데스크탑 Shell | Electron 최신 안정 버전 |
| 인스톨러 빌드 | electron-builder |
| 백엔드 | Spring Boot 3.5 / Java 17 |
| 프론트엔드 | React (CRA) |
| JRE 번들 | jlink (최소 JRE) |
| AI 서버 | Python FastAPI + XGBoost |
| 클라우드 | Railway / Render / AWS EC2 |
| 로컬 DB | SQLite (변경 없음) |
| 버전 배포 | GitHub Releases |

---

## 6. 작업 순서 요약

```
Phase 1  React + Spring Boot 단일 포트 통합     ← 가장 먼저
Phase 2  Electron Shell 구성
Phase 3  AI 서버 클라우드 배포
Phase 4  JRE 번들링 + 인스톨러 빌드
Phase 5  다운로드 사이트 구성
Phase 6  코드 서명 ❌ 제외 (유료)
```

---

## 7. 예상 패키지 크기

| 구성 요소 | 크기 |
|----------|------|
| Electron | ~80MB |
| 최소 JRE (jlink) | ~40MB |
| Spring Boot JAR | ~30MB |
| React 빌드 결과물 | ~5MB |
| **총합** | **~155MB** |

---

## 8. 주의사항

- **macOS 빌드**는 macOS 환경에서만 가능 (`.dmg` 생성 시)
- **Windows 빌드**는 Windows 또는 Docker(Linux)에서 가능
- AI 서버 클라우드 비용 발생 가능 (Railway 무료 플랜 활용 권장)
- `GEMINI_API_KEY`는 AI 서버 환경변수로만 주입 — 앱에 포함 금지
