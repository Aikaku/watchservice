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
