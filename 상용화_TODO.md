# 상용화 전 필요 작업 목록

> 최초 분석: 2026-03-08 | 최종 업데이트: 2026-03-18
> 완료 기준: 외부 배포 / 실사용자 운영 가능 수준

---

## P0 — 즉시 수정 (보안·배포 필수)

### ~~GUI 폴더 선택 제거~~ (완료 2026-03-08)
- ~~JFileChooser Swing 다이얼로그 제거, 서버 파일시스템 탐색 모달로 교체~~
- ~~headless 서버 / Docker / 원격 서버 환경 지원~~

### ~~관리자 비밀번호 BCrypt 해싱~~ (완료 2026-03-09)
- ~~`ADMIN_PASSWORD` 값이 `$2a$`/`$2b$`로 시작하면 BCrypt 검증, 아니면 평문 비교 (기존 배포 호환)~~
- ~~`spring-security-crypto` 의존성 추가~~
- ~~`start.sh`에 JAVA_HOME=Java 17 명시 (Gradle 8.x가 Java 25 미지원)~~

---

## P1 — 단기 수정 (운영 안정성)

### ~~AI 서버 RestTemplate 타임아웃 미설정~~ (완료 2026-03-09)
- ~~`SimpleClientHttpRequestFactory`로 connectTimeout=5s / readTimeout=15s 설정~~
- ~~`AI_CONNECT_TIMEOUT_MS`, `AI_READ_TIMEOUT_MS` 환경변수로 조정 가능~~

### ~~CORS 하드코딩 → 환경변수화~~ (완료 2026-03-09)
- ~~전 컨트롤러 `@CrossOrigin` 어노테이션 제거 (8개 파일)~~
- ~~`WebConfig`에서 `cors.allowed-origins` `@Value` 주입으로 중앙화~~
- ~~`CORS_ALLOWED_ORIGINS` 환경변수로 도메인 지정 가능~~

### ~~HTTPS 미설정~~ (가이드 완료 2026-03-09)
- ~~`application.yml`에 Spring Boot SSL / Nginx 리버스 프록시 설정 방법 주석 추가~~
- ~~실서버 배포 시 Nginx 방법 권장~~

---

## P2 — 중기 수정 (UX·운영 편의)

### ~~알림 통계 페이지 미구현~~ (완료 2026-03-09)
- ~~백엔드 `GET /notifications/stats` (ai_label별 GROUP BY 집계) 구현~~
- ~~`NotificationsApi.js` → 실제 API 호출로 교체, `NotificationStatsPage` 차트 연동~~

### ~~프론트 `alert()` → Toast/인라인 메시지 교체~~ (완료 2026-03-09)
- ~~자체 `Toast.jsx` + `ToastProvider` 구현, 전 페이지 `alert()` 완전 제거~~

### ~~SQLite WAL 모드 미설정~~ (완료 2026-03-09)
- ~~`SqliteConfig` `@PostConstruct`에서 `PRAGMA journal_mode=WAL` 실행~~

### ~~로그 파일 롤링 미설정~~ (완료 2026-03-09)
- ~~`logback-spring.xml` 추가, SizeAndTimeBasedRollingPolicy (50MB/30일/2GB) 설정~~

---

## P3 — 장기 수정 (보안 강화)

### ~~Spring Security 도입~~ (완료 2026-03-09)
- ~~`spring-boot-starter-security` 추가 (`spring-security-crypto` 대체)~~
- ~~`SecurityConfig`: 기존 AdminAuthInterceptor 유지, CSRF disable, 보안 헤더 활성화 (X-Frame-Options DENY, X-Content-Type-Options nosniff 등)~~

### ~~세션 타임아웃 미설정~~ (완료 2026-03-09)
- ~~`server.servlet.session.timeout: 30m` 설정~~

### ~~Actuator 메트릭/헬스체크 미구성~~ (완료 2026-03-09)
- ~~`spring-boot-starter-actuator` 이미 포함됨~~
- ~~`management.endpoints.web.exposure.include: health`로 health만 외부 노출, 상세 정보 숨김~~

---

---

## P4 — 2026-03-18 신규 발굴 (미완료)

### 보안

#### ~~`.env` 파일 git 추적 여부 확인~~ (확인 완료 — `.gitignore`에 포함됨)

#### 관리자 평문 비교 폴백 제거
- `AdminAuthService.java:45`: `return adminPassword.equals(password);` 코드가 여전히 활성화됨
- BCrypt 해싱 완료됐지만 환경변수를 평문으로 설정하면 평문 비교로 동작
- 평문 비교 분기 제거하고 BCrypt만 허용하도록 강제 필요

#### 관리자 기본 비밀번호 폴백 제거
- `application.yml`: `${ADMIN_PASSWORD:123456789}` — 환경변수 미설정 시 약한 기본값으로 서버 기동
- 폴백값 제거 또는 기동 시 환경변수 필수화 처리 필요

---

### 프론트엔드

#### 관리자 페이지 ProtectedRoute 미구현
- `/admin/**` 라우트가 프론트엔드에서 인증 없이 접근 가능
- 백엔드 세션 유효성 확인 후 렌더링하는 ProtectedRoute 컴포넌트 추가 필요
- 관련 파일: `watchservice_fe/src/App.js`

#### `window.location.href` → `useNavigate()` 교체
- `AdminMainPage.jsx:16`에서 로그아웃 시 전체 페이지 새로고침 발생
- React Router `useNavigate()`로 교체해야 SPA 상태 유지

#### HTTP 요청 타임아웃 미설정
- `HttpClient.js`의 `fetch()`에 타임아웃 없음 — AI 서버 무응답 시 무한 대기
- `AbortSignal.timeout(10000)` 추가 필요

#### HTML 메타데이터 CRA 기본값 그대로
- `public/index.html`: title=`React App`, description=`Web site created using create-react-app`
- 서비스 이름과 설명으로 교체 필요

#### 버전 확인 기능 미구현
- `SettingUpdatePage.jsx`: 버전 체크 버튼 클릭 시 "나중에 구현" 토스트만 표시
- 실제 버전 확인 로직 구현 또는 제거 필요

#### 에러 바운더리 없음
- 컴포넌트 런타임 에러 발생 시 전체 앱 크래시
- `ErrorBoundary` 컴포넌트를 `App.js` 최상위에 추가 필요

---

### 백엔드

#### `GlobalExceptionHandler` 없음
- `@ControllerAdvice` 미구현 — 예외가 Spring 기본 에러 응답으로 노출
- 일부 컨트롤러는 try-catch 처리, 일부는 예외 그냥 throw (불일치)

#### API 응답 형식 불일치
- `List`, `String`, `Map.of("result","ok")`, 커스텀 DTO 혼용
- 통일된 `ApiResponse<T>` 래퍼 클래스 도입 필요

#### 입력 검증 없음
- `@Valid`, `@NotNull`, `@NotBlank` 등 Bean Validation 어노테이션 전혀 없음
- `/settings/browse?path=` 파일시스템 접근 파라미터도 무검증
- `spring-boot-starter-validation` 의존성 추가 및 DTO에 어노테이션 적용 필요

---

### AI 서버

#### `requirements.txt` 없음
- `코드/` 폴더에 Python 의존성 파일 미존재
- 필요 라이브러리: `fastapi`, `uvicorn`, `xgboost`, `pandas`, `numpy`, `pyyaml`, `lightgbm`

#### `configs/config.yaml` 없음
- `api_server.py`가 `configs/config.yaml` 참조하지만 파일 미존재
- `OUTPUT_DIR` 경로 지정 불가 — 운영 환경에서 아티팩트 경로 관리 어려움

---

### 프론트엔드 배포 환경변수

#### `.env.production` 파일 없음
- `HttpClient.js`: `REACT_APP_API_BASE_URL || 'http://localhost:8080'` — 환경변수 미설정 시 localhost로 요청
- 프로덕션 빌드 시 실제 서버 주소를 주입할 `.env.production` 파일 또는 CI/CD 환경변수 설정 필요

---

### CORS 운영 설정

#### `application.yml` CORS 기본값에 Vite dev 서버 포함
- `allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}`
- `localhost:5173`은 Vite 개발 서버 주소로 프로덕션에 포함되면 안 됨
- 배포 시 `CORS_ALLOWED_ORIGINS` 환경변수를 실제 도메인으로 반드시 설정 필요

#### AI 서버 CORS `allow_origins=["*"]`
- `api_server.py:47`: 모든 출처에서 호출 가능
- 백엔드 전용 서버이므로 Spring Boot 서버 주소만 허용하도록 제한 필요

---

### 배포 인프라

#### Docker / docker-compose 없음
- Spring Boot, FastAPI, React 3개 서버를 수동으로 각각 실행해야 함
- `docker-compose.yml` 없이는 타인이 프로젝트 실행 불가
- `Dockerfile` × 3, `docker-compose.yml` 구성 필요

---

## 완료 항목 요약

| 날짜 | 항목 |
|------|------|
| 2026-03-08 | JFileChooser 제거 → 서버 파일시스템 탐색 모달로 교체 (P0) |
| 2026-03-08 | `spring.main.headless: true` 설정 |
| 2026-03-08 | Gemini API 타임아웃 10000ms, thinkingBudget=0 설정 |
| 2026-03-08 | `bootRun { environment System.getenv() }` 환경변수 전달 수정 |
| 2026-03-09 | SQLite WAL 모드 (`SqliteConfig` @PostConstruct) (P2) |
| 2026-03-09 | logback-spring.xml 롤링 설정 50MB/30일/2GB (P2) |
| 2026-03-09 | 전 페이지 alert() → Toast 컴포넌트로 교체 (P2) |
| 2026-03-09 | `/notifications/stats` API 구현 + 프론트 차트 연동 (P2) |
| 2026-03-09 | Spring Security 도입, 보안 헤더 활성화 (P3) |
| 2026-03-09 | 세션 타임아웃 30분 설정 (P3) |
| 2026-03-09 | Actuator health 엔드포인트만 외부 노출 설정 (P3) |
