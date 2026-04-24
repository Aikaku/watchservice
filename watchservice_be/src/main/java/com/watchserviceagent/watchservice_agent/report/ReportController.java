package com.watchserviceagent.watchservice_agent.report;

import com.watchserviceagent.watchservice_agent.common.util.OwnerKeyUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 클래스 이름 : ReportController
 * 기능 : 탐지 리포트 PDF 다운로드 엔드포인트를 제공한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 함수 이름 : downloadReport
     * 기능 : 기간별 탐지 리포트를 PDF로 생성하여 다운로드 응답으로 반환한다.
     * 매개변수 : from - 시작 날짜(YYYY-MM-DD, 선택), to - 종료 날짜(YYYY-MM-DD, 선택)
     * 반환값 : 없음 (PDF 바이트를 response에 직접 기록)
     * 작성 날짜 : 2026/04/24
     * 작성자 : 시스템
     */
    @GetMapping
    public void downloadReport(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to",   required = false) String to,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {
        String ownerKey = OwnerKeyUtil.getOrCreate(session);

        log.info("[ReportController] PDF 리포트 요청 ownerKey={}, from={}, to={}", ownerKey, from, to);

        byte[] pdf = reportService.generatePdf(ownerKey, from, to);

        String filename = buildFilename(from, to);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private String buildFilename(String from, String to) {
        String f = (from != null && !from.isBlank()) ? from : "all";
        String t = (to   != null && !to.isBlank())   ? to   : "all";
        return "detection_report_" + f + "_" + t + ".pdf";
    }
}
