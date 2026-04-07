package com.watchserviceagent.watchservice_agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchserviceagent.watchservice_agent.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 클래스 이름 : FamilyInfoController
 * 기능 : 랜섬웨어 패밀리 상세 정보를 반환한다. resources/families/{name}.json 파일을 읽어 제공한다.
 * 작성 날짜 : 2026/04/07
 * 작성자 : 시스템
 */
@RestController
@RequestMapping("/api/families")
@Slf4j
public class FamilyInfoController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping("/{name}")
    public ApiResponse<Map<String, Object>> getFamilyInfo(@PathVariable("name") String name) {
        // 경로 순회 방지: 파일명에 / \ . 가 2개 이상 들어오면 차단
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new NoSuchElementException("패밀리 정보를 찾을 수 없습니다: " + name);
        }

        String resourcePath = "families/" + name + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                throw new NoSuchElementException("패밀리 정보를 찾을 수 없습니다: " + name);
            }
            try (InputStream is = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = MAPPER.readValue(is, Map.class);
                log.info("[FamilyInfoController] GET /api/families/{} 조회 완료", name);
                return ApiResponse.ok(data);
            }
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[FamilyInfoController] 패밀리 정보 조회 실패: {}", name, e);
            throw new NoSuchElementException("패밀리 정보를 찾을 수 없습니다: " + name);
        }
    }
}
