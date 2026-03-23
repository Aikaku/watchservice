# 상용화 전 필요 작업 목록

> 최초 분석: 2026-03-08 | 최종 업데이트: 2026-03-18
> 완료 기준: 외부 배포 / 실사용자 운영 가능 수준

---

## 외부 공개 서비스 필수 항목

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ⬜ | **일반 사용자 인증 구현** — `/notifications`, `/logs`, `/settings` 등 일반 사용자 페이지 전체에 로그인 없이 접근 가능. 사용자 회원가입·로그인 및 인증 미완료 시 접근 차단 구현 필요 | — |
| ⬜ | **HTTPS 실제 적용** — `application.yml`에 가이드 주석만 존재, 실제 SSL 미적용. Let's Encrypt 인증서 발급 후 Nginx 리버스 프록시 또는 Spring Boot SSL 실제 설정 필요 | — |

---

## 보안

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ✅ | 관리자 비밀번호 BCrypt 해싱 — `ADMIN_PASSWORD`가 `$2a$`/`$2b$`로 시작하면 BCrypt 검증 | 2026-03-09 |
| ✅ | Spring Security 도입 — CSRF disable, 보안 헤더 활성화 (X-Frame-Options DENY, X-Content-Type-Options nosniff 등) | 2026-03-09 |
| ✅ | 세션 타임아웃 30분 설정 — `server.servlet.session.timeout: 30m` | 2026-03-09 |
| ✅ | `.env` 파일 git 추적 여부 확인 — `.gitignore`에 포함 확인 | 2026-03-18 |
| ⬜ | **관리자 평문 비교 폴백 제거** — `AdminAuthService.java:45`의 `adminPassword.equals(password)` 제거, BCrypt만 허용 | — |
| ⬜ | **관리자 기본 비밀번호 폴백 제거** — `application.yml`의 `${ADMIN_PASSWORD:123456789}` 폴백 제거 또는 기동 시 환경변수 필수화 | — |

---

## 백엔드

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ✅ | JFileChooser Swing 다이얼로그 제거 → 서버 파일시스템 탐색 모달로 교체 | 2026-03-08 |
| ✅ | `spring.main.headless: true` 설정 — headless 서버·Docker·원격 환경 지원 | 2026-03-08 |
| ✅ | `bootRun { environment System.getenv() }` — 환경변수 전달 수정 | 2026-03-08 |
| ✅ | AI 서버 RestTemplate 타임아웃 설정 — connectTimeout=5s / readTimeout=15s, 환경변수 조정 가능 | 2026-03-09 |
| ✅ | CORS 하드코딩 → 환경변수화 — `@CrossOrigin` 제거, `WebConfig`에서 `CORS_ALLOWED_ORIGINS`로 중앙화 | 2026-03-09 |
| ✅ | SQLite WAL 모드 — `SqliteConfig` `@PostConstruct`에서 `PRAGMA journal_mode=WAL` 실행 | 2026-03-09 |
| ✅ | 로그 파일 롤링 — `logback-spring.xml` SizeAndTimeBasedRollingPolicy (50MB / 30일 / 2GB) | 2026-03-09 |
| ✅ | Actuator health 엔드포인트만 외부 노출 — 상세 정보 숨김 | 2026-03-09 |
| ✅ | `/notifications/stats` API 구현 — ai_label별 GROUP BY 집계 | 2026-03-09 |
| ⬜ | **`GlobalExceptionHandler` 없음** — `@ControllerAdvice` 미구현, 일부 컨트롤러 예외 처리 불일치 | — |
| ⬜ | **API 응답 형식 불일치** — `List`, `String`, `Map.of("result","ok")`, 커스텀 DTO 혼용 → 통일된 `ApiResponse<T>` 래퍼 도입 | — |
| ⬜ | **입력 검증 없음** — `@Valid`, `@NotNull` 등 Bean Validation 전무, `/settings/browse?path=` 파일시스템 파라미터 무검증 | — |

---

## 프론트엔드

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ✅ | `alert()` → Toast 컴포넌트 교체 — 자체 `Toast.jsx` + `ToastProvider` 구현, 전 페이지 적용 | 2026-03-09 |
| ✅ | 알림 통계 페이지 — `NotificationsApi.js` 실제 API 호출 연동, `NotificationStatsPage` 차트 구현 | 2026-03-09 |
| ⬜ | **관리자 페이지 ProtectedRoute 미구현** — `/admin/**` 프론트에서 인증 없이 접근 가능, `App.js`에 ProtectedRoute 추가 필요 | — |
| ⬜ | **`window.location.href` → `useNavigate()` 교체** — `AdminMainPage.jsx:16` 로그아웃 시 전체 새로고침 발생 | — |
| ⬜ | **HTTP 요청 타임아웃 미설정** — `HttpClient.js`의 `fetch()`에 타임아웃 없음, `AbortSignal.timeout(10000)` 추가 필요 | — |
| ⬜ | **HTML 메타데이터 CRA 기본값** — `public/index.html` title=`React App`, description 미변경 | — |
| ⬜ | **버전 확인 기능 미구현** — `SettingUpdatePage.jsx` 버전 체크 버튼이 "나중에 구현" 토스트만 표시 | — |
| ⬜ | **에러 바운더리 없음** — 컴포넌트 런타임 에러 시 전체 앱 크래시, `App.js` 최상위에 `ErrorBoundary` 필요 | — |
| ⬜ | **`.env.production` 파일 없음** — `REACT_APP_API_BASE_URL` 기본값이 `localhost:8080`, 프로덕션 배포 시 실서버 주소 주입 방법 없음 | — |

---

## AI 서버

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ✅ | Gemini API 타임아웃 10000ms, thinkingBudget=0 설정 | 2026-03-08 |
| ⬜ | **`requirements.txt` 없음** — 필요 라이브러리: `fastapi`, `uvicorn`, `xgboost`, `lightgbm`, `pandas`, `numpy`, `pyyaml` | — |
| ⬜ | **`configs/config.yaml` 없음** — `api_server.py`가 참조하나 파일 미존재, 운영 환경 아티팩트 경로 관리 불가 | — |
| ✅ | CORS `allow_origins=["*"]` → `CORS_ALLOWED_ORIGINS` 환경변수 기반으로 수정 | 2026-03-21 |
| ✅ | 다중 클래스 score 계산 오류 수정 — 랜섬웨어 총합 확률 합산 방식으로 변경 | 2026-03-21 |
| ✅ | benign 판정 로직 수정 — score 임계값 기반(≥0.70 DANGER / ≥0.50 WARNING)으로 변경 | 2026-03-21 |
| ⬜ | **누락 피처 -1 채움** — `features.get(c, -1)` 학습 시 결측 처리 방식과 불일치 가능 | — |
| ⬜ | **모델 로드 실패 시 서버 조용히 기동** — 모델 없이 기동 시 항상 `UNKNOWN` 반환, 명시적 실패 처리 또는 `/health` 503 반환 필요 | — |

---

## 배포 인프라·환경설정

| 상태 | 항목 | 완료일 |
|------|------|--------|
| ✅ | HTTPS 가이드 완료 — `application.yml`에 Spring Boot SSL / Nginx 리버스 프록시 설정 방법 주석 추가 | 2026-03-09 |
| ⬜ | **CORS 기본값에 Vite dev 서버 포함** — `allowed-origins`에 `localhost:5173` 포함, 배포 시 `CORS_ALLOWED_ORIGINS` 환경변수 실제 도메인으로 설정 필수 | — |
| ⬜ | **Docker / docker-compose 없음** — Spring Boot·FastAPI·React 3개 서버 수동 실행 필요, `Dockerfile` × 3 + `docker-compose.yml` 구성 필요 | — |
