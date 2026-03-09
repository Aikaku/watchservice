# 상용화 전 필요 작업 목록

> 분석 날짜: 2026-03-08
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

### ☐ AI 서버 RestTemplate 타임아웃 미설정
- **현재**: `RestTemplate` 기본값 → 무한 블로킹 위험
- **수정 방향**: `SimpleClientHttpRequestFactory` 또는 `HttpComponentsClientHttpRequestFactory`로 connect/read 타임아웃 설정 (예: 5s / 15s)
- **관련 파일**: `ai/AiService.java` 또는 RestTemplate Bean 설정

### ☐ CORS 하드코딩 → 환경변수화
- **현재**: `@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})` 전 컨트롤러에 하드코딩
- **수정 방향**: `application.yml`에 `cors.allowed-origins` 추가, `WebMvcConfigurer`로 중앙화
- **관련 파일**: 모든 `@RestController` 클래스, `application.yml`

### ☐ HTTPS 미설정
- **현재**: HTTP만 지원 (포트 8080)
- **수정 방향**: Let's Encrypt SSL 인증서 + `server.ssl.*` 설정, 또는 Nginx 리버스 프록시로 TLS 종단
- **관련 파일**: `application.yml`, 배포 환경 설정

---

## P2 — 중기 수정 (UX·운영 편의)

### ☐ 알림 통계 페이지 미구현
- **현재**: `fetchAlertStats` 항상 빈 배열 반환
- **수정 방향**: 백엔드 `/notifications/stats` 집계 API 구현, 프론트 차트 연동
- **관련 파일**: `alerts/NotificationController.java`, `NotificationsApi.js`, `notifications/stats` 페이지

### ☐ 프론트 `alert()` → Toast/인라인 메시지 교체
- **현재**: 15곳 이상 브라우저 기본 `alert()` 사용 → UX 저하, 일부 환경 차단
- **수정 방향**: Toast 라이브러리(예: react-hot-toast) 또는 자체 Toast 컴포넌트로 교체
- **관련 파일**: `watchservice_fe/src` 전반

### ☐ SQLite WAL 모드 미설정
- **현재**: 기본 저널 모드 → 다중 접근 시 잠금 충돌 가능
- **수정 방향**: 앱 시작 시 `PRAGMA journal_mode=WAL` 실행
- **관련 파일**: SQLite 초기화 코드 (DataSource 설정 또는 `@PostConstruct`)

### ☐ 로그 파일 롤링 미설정
- **현재**: `logback-spring.xml` 없음 → 콘솔 출력만, 장기 운영 시 로그 소실
- **수정 방향**: `logback-spring.xml` 추가, RollingFileAppender (일별/크기별) 설정
- **관련 파일**: `src/main/resources/logback-spring.xml` (신규)

---

## P3 — 장기 수정 (보안 강화)

### ☐ Spring Security 도입
- **현재**: 커스텀 `AdminAuthInterceptor`만 존재 — CSRF, XSS, 클릭재킹 방어 없음
- **수정 방향**: `spring-boot-starter-security` 추가, SecurityFilterChain 설정, CSRF 토큰 활성화

### ☐ 세션 타임아웃 미설정
- **현재**: 서버 재시작 전까지 세션 만료 없음
- **수정 방향**: `application.yml`에 `server.servlet.session.timeout: 30m` 추가

### ☐ Actuator 메트릭/헬스체크 미구성
- **현재**: `/actuator` 엔드포인트 비활성화 또는 미설정
- **수정 방향**: `spring-boot-starter-actuator` 추가, `/actuator/health` 외부 노출, 나머지 엔드포인트 인증 보호

---

## 완료 항목 요약

| 날짜 | 항목 |
|------|------|
| 2026-03-08 | JFileChooser 제거 → 서버 파일시스템 탐색 모달로 교체 (P0) |
| 2026-03-08 | `spring.main.headless: true` 설정 |
| 2026-03-08 | Gemini API 타임아웃 10000ms, thinkingBudget=0 설정 |
| 2026-03-08 | `bootRun { environment System.getenv() }` 환경변수 전달 수정 |
