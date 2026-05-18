package com.watchserviceagent.watchservice_agent.report;

import com.watchserviceagent.watchservice_agent.alerts.NotificationRepository;
import com.watchserviceagent.watchservice_agent.alerts.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 클래스 이름 : ReportService
 * 기능 : 탐지 이력을 기간별로 집계하여 PDF 리포트를 생성한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 함수 이름 : generatePdf
     * 기능 : 지정 기간의 알림 데이터를 조회하여 PDF 바이트 배열로 반환한다.
     * 매개변수 : ownerKey - 소유자 키, from - 시작 날짜(YYYY-MM-DD), to - 종료 날짜(YYYY-MM-DD)
     * 반환값 : byte[] - PDF 파일 바이트 배열
     */
    public byte[] generatePdf(String ownerKey, String from, String to) throws IOException {
        Long fromEpoch = parseFromEpoch(from);
        Long toEpoch = parseToEpoch(to);

        List<Notification> items = notificationRepository.findNotificationsByOwner(
                ownerKey, fromEpoch, toEpoch, null, null,
                "createdAt", "desc", 0, 1000
        );

        long total   = items.size();
        long danger  = items.stream().filter(n -> "DANGER".equals(n.getAiLabel())).count();
        long warning = items.stream().filter(n -> "WARNING".equals(n.getAiLabel())).count();
        long safe    = items.stream().filter(n -> "SAFE".equals(n.getAiLabel())).count();

        String reportFrom = from != null ? from : "ALL";
        String reportTo   = to   != null ? to   : "ALL";

        try (PDDocument doc = new PDDocument()) {
            // ─── 1페이지: 요약 ───
            PDPage summaryPage = new PDPage(PDRectangle.A4);
            doc.addPage(summaryPage);
            writeSummaryPage(doc, summaryPage, reportFrom, reportTo, total, danger, warning, safe);

            // ─── 2페이지~: 탐지 목록 ───
            writeDetailPages(doc, items);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            log.info("[ReportService] PDF 생성 완료: ownerKey={}, from={}, to={}, total={}건", ownerKey, from, to, total);
            return baos.toByteArray();
        }
    }

    private void writeSummaryPage(PDDocument doc, PDPage page,
                                  String from, String to,
                                  long total, long danger, long warning, long safe) throws IOException {
        PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = 760;

            // 제목
            cs.beginText();
            cs.setFont(bold, 20);
            cs.newLineAtOffset(50, y);
            cs.showText("WatchService Agent - Detection Report");
            cs.endText();
            y -= 30;

            // 기간
            cs.beginText();
            cs.setFont(regular, 12);
            cs.newLineAtOffset(50, y);
            cs.showText("Period : " + from + "  ~  " + to);
            cs.endText();
            y -= 16;

            // 생성 시각
            String generatedAt = DATE_FMT.format(Instant.now());
            cs.beginText();
            cs.setFont(regular, 10);
            cs.newLineAtOffset(50, y);
            cs.showText("Generated : " + generatedAt);
            cs.endText();
            y -= 30;

            // 구분선
            cs.setLineWidth(0.5f);
            cs.moveTo(50, y);
            cs.lineTo(545, y);
            cs.stroke();
            y -= 24;

            // 요약 통계
            cs.beginText();
            cs.setFont(bold, 14);
            cs.newLineAtOffset(50, y);
            cs.showText("Summary");
            cs.endText();
            y -= 22;

            String[][] rows = {
                    {"Total Alerts",   String.valueOf(total)},
                    {"DANGER",         String.valueOf(danger)},
                    {"WARNING",        String.valueOf(warning)},
                    {"SAFE",           String.valueOf(safe)},
            };
            for (String[] row : rows) {
                cs.beginText();
                cs.setFont(regular, 12);
                cs.newLineAtOffset(60, y);
                cs.showText(row[0]);
                cs.endText();

                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(220, y);
                cs.showText(row[1]);
                cs.endText();
                y -= 18;
            }
        }
    }

    private void writeDetailPages(PDDocument doc, List<Notification> items) throws IOException {
        if (items.isEmpty()) return;

        PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        float y = 760;
        int pageNum = 2;

        // 탐지 목록 헤더
        cs.beginText();
        cs.setFont(bold, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Detection List  (Page " + pageNum + ")");
        cs.endText();
        y -= 20;

        // 컬럼 헤더
        cs.setLineWidth(0.3f);
        cs.moveTo(50, y);
        cs.lineTo(545, y);
        cs.stroke();
        y -= 14;

        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(50, y);
        cs.showText("Date/Time");
        cs.endText();
        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(195, y);
        cs.showText("Level");
        cs.endText();
        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(255, y);
        cs.showText("Score");
        cs.endText();
        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(315, y);
        cs.showText("Family");
        cs.endText();
        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(430, y);
        cs.showText("Files");
        cs.endText();

        y -= 4;
        cs.moveTo(50, y);
        cs.lineTo(545, y);
        cs.stroke();
        y -= 14;

        for (Notification n : items) {
            // 페이지 넘김
            if (y < 60) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                pageNum++;
                y = 760;

                cs.beginText();
                cs.setFont(bold, 14);
                cs.newLineAtOffset(50, y);
                cs.showText("Detection List  (Page " + pageNum + ")");
                cs.endText();
                y -= 30;
            }

            String createdAt = DATE_FMT.format(n.getCreatedAt());
            String label  = n.getAiLabel() != null ? n.getAiLabel() : "UNKNOWN";
            String score  = n.getAiScore() != null ? String.format("%.1f%%", n.getAiScore() * 100) : "-";
            String family = n.getTopFamily() != null ? truncate(n.getTopFamily(), 14) : "-";
            String files  = String.valueOf(n.getAffectedFilesCount());
            String fp     = n.isFalsePositive() ? " [FP]" : "";

            cs.beginText();
            cs.setFont(regular, 8);
            cs.newLineAtOffset(50, y);
            cs.showText(createdAt);
            cs.endText();

            cs.beginText();
            cs.setFont(bold, 8);
            cs.newLineAtOffset(195, y);
            cs.showText(label + fp);
            cs.endText();

            cs.beginText();
            cs.setFont(regular, 8);
            cs.newLineAtOffset(255, y);
            cs.showText(score);
            cs.endText();

            cs.beginText();
            cs.setFont(regular, 8);
            cs.newLineAtOffset(315, y);
            cs.showText(family);
            cs.endText();

            cs.beginText();
            cs.setFont(regular, 8);
            cs.newLineAtOffset(430, y);
            cs.showText(files);
            cs.endText();

            y -= 13;
        }

        cs.close();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }

    private Long parseFromEpoch(String from) {
        if (from == null || from.isBlank()) return null;
        try {
            return LocalDate.parse(from.trim(), DATE_ONLY)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) { return null; }
    }

    private Long parseToEpoch(String to) {
        if (to == null || to.isBlank()) return null;
        try {
            return LocalDate.parse(to.trim(), DATE_ONLY)
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) { return null; }
    }
}
