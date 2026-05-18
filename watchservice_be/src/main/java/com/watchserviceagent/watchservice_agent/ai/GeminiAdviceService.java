package com.watchserviceagent.watchservice_agent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchserviceagent.watchservice_agent.ai.domain.AiResult;
import com.watchserviceagent.watchservice_agent.ai.dto.AiPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GeminiAdviceService
 * - SAFE: Gemini 호출 금지, 고정 문구 반환
 * - WARNING/DANGER: Gemini 호출
 * - 429(쿼터/레이트리밋): quota 안내 + 기본 대응 체크리스트 반환
 * - 기타 실패: fallback 체크리스트 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAdviceService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key:}")
    private String apiKey;

    /**
     * 예: gemini-2.5-flash 또는 models/gemini-2.5-flash 둘 다 허용
     */
    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.timeout-ms:2500}")
    private int timeoutMs;

    /*
     * 함수 이름 : buildRestTemplate
     * 기능 : 설정된 타임아웃을 적용한 RestTemplate을 생성한다.
     * 매개변수 : 없음
     * 반환값 : RestTemplate - 타임아웃이 적용된 RestTemplate
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        return new RestTemplate(f);
    }

    /*
     * 함수 이름 : guidanceFor
     * 기능 : AI 분석 결과를 바탕으로 Gemini LLM에 대응 가이드 생성을 요청한다. SAFE이면 고정 문구, 실패 시 fallback을 반환한다.
     * 매개변수 : payload - AI 피처 데이터, aiResult - AI 분석 결과
     * 반환값 : String - 생성된 대응 가이드 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public String guidanceFor(AiPayload payload, AiResult aiResult) {

        // ✅ null-safe 로그
        String labelForLog = (aiResult == null) ? "null" : String.valueOf(aiResult.getLabel());
        log.info("[GeminiAdviceService] label={}, apiKeyPresent={}",
                labelForLog, (apiKey != null && !apiKey.isBlank()));

        // 1) AI 결과 없으면 fallback
        if (aiResult == null || aiResult.getLabel() == null) {
            return fallbackGuidance(payload, aiResult);
        }

        String label = aiResult.getLabel().trim().toUpperCase();

        // 2) SAFE: Gemini 호출 금지 + 한 문장 고정
        if ("SAFE".equals(label)) {
            return "현재 상태는 정상 범주로 판단됩니다. 모니터링을 유지하세요.";
        }

        // 3) WARNING / DANGER만 Gemini 호출
        if (!"WARNING".equals(label) && !"DANGER".equals(label)) {
            return fallbackGuidance(payload, aiResult);
        }

        // 4) API Key 없으면 fallback
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackGuidance(payload, aiResult);
        }

        try {
            String prompt = buildPrompt(payload, aiResult);
            String text = callGemini(prompt);

            if (text == null || text.isBlank()) {
                return fallbackGuidance(payload, aiResult);
            }

            return text.trim();

        } catch (HttpClientErrorException.TooManyRequests e) {
            // ✅ 429: 쿼터/레이트리밋
            log.warn("[GeminiAdviceService] 429 TooManyRequests -> quota/rate limit. body={}", safeBody(e));
            return quotaGuidance();

        } catch (HttpClientErrorException e) {
            // ✅ 나머지 4xx
            log.warn("[GeminiAdviceService] Gemini HTTP error: status={}, body={}",
                    e.getStatusCode(), safeBody(e));
            return fallbackGuidance(payload, aiResult);

        } catch (Exception e) {
            log.warn("[GeminiAdviceService] guidance 생성 실패 -> fallback 사용. reason={}", e.toString());
            return fallbackGuidance(payload, aiResult);
        }
    }

    /*
     * 함수 이름 : buildPrompt
     * 기능 : AI 분석 결과와 피처 데이터를 기반으로 Gemini에 전송할 프롬프트를 생성한다.
     * 매개변수 : payload - AI 피처 데이터, aiResult - AI 분석 결과
     * 반환값 : String - Gemini 프롬프트 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String buildPrompt(AiPayload payload, AiResult aiResult) {
        String label = safe(aiResult.getLabel());
        String score = (aiResult.getScore() == null) ? "-" : String.format("%.2f", aiResult.getScore());
        String detail = safe(aiResult.getDetail());
        String featureSummary = summarizePayload(payload);

        return """
SOC 방어조치 전문가. 수비 관점 조치만 작성(공격정보·코드 제외). 한국어.

탐지: %s score=%s %s
피처: %s

[즉시 조치]
[1시간 내]
[오늘 내]
[비고] 1~2문장
""".formatted(label, score, detail, featureSummary);
    }

    /*
     * 함수 이름 : summarizePayload
     * 기능 : AiPayload의 주요 피처 값을 간략한 문자열로 요약한다.
     * 매개변수 : payload - 요약할 AI 피처 데이터
     * 반환값 : String - 피처 요약 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String summarizePayload(AiPayload payload) {
        if (payload == null) return "-";
        StringBuilder sb = new StringBuilder();
        if (payload.getFileReadCount() > 0)      sb.append("read=").append(payload.getFileReadCount()).append(' ');
        if (payload.getFileWriteCount() > 0)     sb.append("write=").append(payload.getFileWriteCount()).append(' ');
        if (payload.getFileDeleteCount() > 0)    sb.append("del=").append(payload.getFileDeleteCount()).append(' ');
        if (payload.getFileRenameCount() > 0)    sb.append("ren=").append(payload.getFileRenameCount()).append(' ');
        if (payload.getFileEncryptLikeCount() > 0) sb.append("enc=").append(payload.getFileEncryptLikeCount()).append(' ');
        if (payload.getChangedFilesCount() > 0)  sb.append("files=").append(payload.getChangedFilesCount()).append(' ');
        if (payload.getRandomExtensionFlag() > 0) sb.append("randExt=1 ");
        if (payload.getEntropyDiffMean() > 0.001) sb.append(String.format("entrDiff=%.3f ", payload.getEntropyDiffMean()));
        if (Math.abs(payload.getFileSizeDiffMean()) > 0.5) sb.append(String.format("sizeDiff=%.0f ", payload.getFileSizeDiffMean()));
        return sb.isEmpty() ? "변화없음" : sb.toString().trim();
    }

    /*
     * 함수 이름 : callGemini
     * 기능 : Gemini REST API를 호출하여 생성된 텍스트를 반환한다.
     * 매개변수 : prompt - Gemini에 전달할 프롬프트 문자열
     * 반환값 : String - Gemini 응답 텍스트, 실패 시 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String callGemini(String prompt) {
        log.info("[GeminiAdviceService] calling Gemini model={} timeoutMs={}", model, timeoutMs);

        // ✅ model에 models/가 들어와도 안전하게 처리
        String m = (model == null) ? "" : model.trim();
        if (m.startsWith("models/")) {
            m = m.substring("models/".length());
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + m + ":generateContent?key=" + apiKey;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", new Object[]{
                Map.of(
                        "role", "user",
                        "parts", new Object[]{Map.of("text", prompt)}
                )
        });
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 1500);
        generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        body.put("generationConfig", generationConfig);

        // ✅ “Gemini로 보내는 JSON” 로그로 보기 (직렬화 실패해도 호출은 계속)
        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            // 너무 길면 잘라서 로그(원하면 길이 더 줄여도 됨)
            log.info("[GeminiAdviceService] Gemini request body={}", cut(bodyJson, 4000));
        } catch (Exception e) {
            log.warn("[GeminiAdviceService] Failed to serialize request body for logging: {}", e.toString());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        RestTemplate rt = buildRestTemplate();
        ResponseEntity<String> res = rt.postForEntity(url, entity, String.class);

        // 혹시 모를 방어(대부분 RestTemplate이 4xx/5xx면 예외 던짐)
        if (!res.getStatusCode().is2xxSuccessful()) {
            throw HttpClientErrorException.create(
                    res.getStatusCode(),
                    "Gemini HTTP " + res.getStatusCode(),
                    res.getHeaders(),
                    (res.getBody() == null ? new byte[0] : res.getBody().getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8
            );
        }

        String raw = res.getBody();
        if (raw == null || raw.isBlank()) return null;

        // candidates[0].content.parts[0].text 추출
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String out = parts.get(0).path("text").asText(null);
                    if (out != null && !out.isBlank()) return out;
                }
            }
        } catch (Exception parseFail) {
            log.debug("[GeminiAdviceService] Gemini JSON 파싱 실패 -> null 반환. reason={}", parseFail.toString());
            return null;
        }

        return null;
    }

    /*
     * 함수 이름 : quotaGuidance
     * 기능 : Gemini API 429 (쿼터/레이트리밋) 발생 시 반환할 기본 대응 안내 문자열을 반환한다.
     * 매개변수 : 없음
     * 반환값 : String - 쿼터 초과 안내 및 기본 대응 체크리스트
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String quotaGuidance() {
        return """
Gemini 가이던스 생성이 현재 사용량/요금제 제한(429)으로 차단되었습니다.
아래 기본 대응 절차를 먼저 진행하세요.

[즉시 조치]
- 의심 파일/프로세스가 있는 경우 해당 PC를 네트워크에서 분리하세요(유선/무선).
- 최근 변경된 파일(특히 확장자 변경/대량 수정)이 있는 경로를 우선 확인하세요.
- 백업 저장소(공유 폴더/NAS/클라우드) 접근 권한을 임시로 제한하세요.
- 증적 보존을 위해 로그/이벤트를 삭제하지 말고 별도 보관하세요.

[1시간 내]
- 엔드포인트 보안 제품(백신/EDR) 최신 업데이트 후 전체 검사 실행을 검토하세요.
- 의심 계정(관리자 포함) 비밀번호 변경 및 MFA 적용 여부를 점검하세요.
- 동일 네트워크 구간의 다른 장비에서 유사 징후가 있는지 확인하세요.

[오늘 내]
- 백업 무결성 점검 및 복구 시나리오(우선순위) 재확인.
- 주요 서버/공유폴더 접근 로그를 분석해 확산 여부를 확인.
- 조직 내 보고/대응 체계에 따라 보안 담당자/관리자에게 상황 공유.
""".trim();
    }

    /*
     * 함수 이름 : fallbackGuidance
     * 기능 : Gemini 호출 실패 시 반환할 기본 대응 체크리스트 문자열을 반환한다.
     * 매개변수 : 없음
     * 반환값 : String - 기본 대응 체크리스트
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String fallbackGuidance(AiPayload payload, AiResult aiResult) {
        String label = (aiResult != null && aiResult.getLabel() != null) ? aiResult.getLabel() : "UNKNOWN";
        String score = (aiResult != null && aiResult.getScore() != null)
                ? String.format("%.0f%%", aiResult.getScore() * 100) : "-";
        String featureSummary = summarizePayload(payload);
        int changedFiles = (payload != null) ? payload.getChangedFilesCount() : 0;
        boolean hasEncrypt = (payload != null) && payload.getFileEncryptLikeCount() > 0;
        boolean hasRename = (payload != null) && payload.getFileRenameCount() > 0;

        StringBuilder sb = new StringBuilder();
        sb.append("탐지 등급: ").append(label)
          .append("  |  AI 점수: ").append(score).append("\n");
        if (!featureSummary.equals("변화없음")) {
            sb.append("감지된 활동: ").append(featureSummary).append("\n");
        }
        if (changedFiles > 0) {
            sb.append("영향 파일: ").append(changedFiles).append("개\n");
        }
        sb.append("\n");

        sb.append("[즉시 조치]\n");
        if (hasEncrypt) {
            sb.append("- 파일 암호화 의심 활동이 감지되었습니다. 해당 PC를 즉시 네트워크에서 분리하세요.\n");
        }
        if (hasRename) {
            sb.append("- 대량 파일 이름 변경이 감지되었습니다. 영향 파일 목록을 즉시 확인하세요.\n");
        }
        sb.append("""
- 의심 파일/프로세스가 있는 경우 해당 PC를 네트워크에서 분리하세요(유선/무선).
- 최근 변경된 파일(특히 확장자 변경/대량 수정)이 있는 경로를 우선 확인하세요.
- 백업 저장소(공유 폴더/NAS/클라우드) 접근 권한을 임시로 제한하세요.
- 증적 보존을 위해 로그/이벤트를 삭제하지 말고 별도 보관하세요.

[1시간 내]
- 엔드포인트 보안 제품(백신/EDR) 최신 업데이트 후 전체 검사 실행을 검토하세요.
- 의심 계정(관리자 포함) 비밀번호 변경 및 MFA 적용 여부를 점검하세요.
- 동일 네트워크 구간의 다른 장비에서 유사 징후가 있는지 확인하세요.

[오늘 내]
- 백업 무결성 점검 및 복구 시나리오(우선순위) 재확인.
- 주요 서버/공유폴더 접근 로그를 분석해 확산 여부를 확인.
- 조직 내 보고/대응 체계에 따라 보안 담당자/관리자에게 상황 공유.

[비고]
AI 분석 가이드를 생성할 수 없어 기본 체크리스트를 제공합니다. Gemini API 키 설정 시 탐지 패턴에 맞는 맞춤형 가이드를 제공받을 수 있습니다.
""");
        return sb.toString().trim();
    }

    /*
     * 함수 이름 : safe
     * 기능 : null 문자열을 "-"로 대체하여 반환한다.
     * 매개변수 : s - 변환할 문자열
     * 반환값 : String - null이면 "-", 그렇지 않으면 원래 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String safe(String s) {
        return (s == null) ? "-" : s;
    }

    /*
     * 함수 이름 : safeBody
     * 기능 : HttpClientErrorException에서 응답 body 문자열을 안전하게 추출한다. 1000자를 초과하면 잘라 반환한다.
     * 매개변수 : e - HTTP 클라이언트 오류 예외
     * 반환값 : String - 응답 body 문자열 또는 "-"
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String safeBody(HttpClientErrorException e) {
        try {
            String b = e.getResponseBodyAsString();
            if (b == null) return "-";
            return b.length() > 1000 ? b.substring(0, 1000) + "..." : b;
        } catch (Exception ignore) {
            return "-";
        }
    }

    /*
     * 함수 이름 : cut
     * 기능 : 문자열이 max 길이를 초과하면 잘라 "..."을 붙여 반환한다.
     * 매개변수 : s - 원본 문자열, max - 최대 허용 길이
     * 반환값 : String - 잘린 문자열 또는 원본 문자열
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    private String cut(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
