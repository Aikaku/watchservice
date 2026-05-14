package com.watchserviceagent.watchservice_agent.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * AI 서버 응답 DTO (두 형식 모두 수용)
 *
 * (A) analyze 형식(기존):
 * {
 *   "status": "ok",
 *   "label": "DANGER" | "WARNING" | "SAFE",
 *   "score": 0.92,
 *   "detail": "top_family=LockBit",
 *   "message": "..."
 * }
 *
 * (B) family/predict 형식(현재 네가 보여준 것):
 * {
 *   "topk":[{"family":"Benign","prob":0.99}, ...],
 *   "message":"Missing ..."
 * }
 */
@Getter
@NoArgsConstructor
@ToString
public class AiResponse {

    private String status;

    // (A) 형식
    private String label;
    private Double score;
    private String detail;
    private String message;

    // (B) 형식
    private List<TopK> topk;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class TopK {
        private String family;
        private Double prob;
    }

    /*
     * 함수 이름 : getTopFamily
     * 기능 : 응답에서 top_family를 추출한다. detail 필드에 파싱을 시도하고, 없으면 topk[0].family를 반환한다.
     * 매개변수 : 없음
     * 반환값 : String - 최상위 패밀리명, 없으면 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public String getTopFamily() {
        // 1) detail에서 파싱
        if (detail != null && !detail.isBlank()) {
            String prefix = "top_family=";
            int idx = detail.indexOf(prefix);
            if (idx >= 0) {
                String after = detail.substring(idx + prefix.length()).trim();
                int endIdx = after.indexOf(',');
                if (endIdx > 0) after = after.substring(0, endIdx).trim();
                if (!after.isBlank()) return after;
            }
        }

        // 2) topk에서 추출
        if (topk != null && !topk.isEmpty()) {
            String fam = topk.get(0).getFamily();
            if (fam != null && !fam.isBlank()) return fam.trim();
        }
        return null;
    }

    /*
     * 함수 이름 : getTopProb
     * 기능 : 응답에서 최상위 확률을 추출한다. score가 있으면 score를, 없으면 topk[0].prob를 반환한다.
     * 매개변수 : 없음
     * 반환값 : Double - 최상위 확률, 없으면 null
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public Double getTopProb() {
        if (score != null) return score;
        if (topk != null && !topk.isEmpty()) {
            return topk.get(0).getProb();
        }
        return null;
    }

    /*
     * 함수 이름 : isRansomware
     * 기능 : topFamily가 Benign이면 false, 그 외는 true를 반환한다.
     * 매개변수 : 없음
     * 반환값 : boolean - 랜섬웨어 여부
     * 작성 날짜 : 2026/03/08
     * 작성자 : 시스템
     */
    public boolean isRansomware() {
        String topFamily = getTopFamily();
        if (topFamily == null || topFamily.isBlank()) return false;
        return !"Benign".equalsIgnoreCase(topFamily.trim());
    }
}
