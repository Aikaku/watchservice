package com.watchserviceagent.watchservice_agent.alerts;

import com.watchserviceagent.watchservice_agent.alerts.domain.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 클래스 이름 : EmailNotificationService
 * 기능 : DANGER 탐지 시 설정된 이메일 주소로 경보 메일을 비동기 발송한다.
 *        SMTP 설정이 없거나 이메일 미설정 시 자동으로 발송을 건너뛴다.
 * 작성 날짜 : 2026/04/07
 * 작성자 : 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * DANGER 알림 이메일을 비동기로 발송한다.
     * recipientEmail 또는 SMTP 계정이 비어 있으면 발송을 건너뛴다.
     */
    @Async
    public void sendDangerAlert(String recipientEmail, Notification notification) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[EmailNotificationService] 수신 이메일 미설정 — 발송 건너뜀");
            return;
        }
        if (smtpUsername == null || smtpUsername.isBlank()) {
            log.warn("[EmailNotificationService] SMTP 계정 미설정 — 발송 건너뜀");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(smtpUsername);
            helper.setTo(recipientEmail);
            helper.setSubject("[WatchService] DANGER — 랜섬웨어 의심 활동 탐지");
            helper.setText(buildHtmlBody(notification), true);

            mailSender.send(message);
            log.info("[EmailNotificationService] DANGER 알림 이메일 발송 완료 → {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("[EmailNotificationService] 이메일 발송 실패 → {}", recipientEmail, e);
        }
    }

    /**
     * 테스트 이메일을 동기로 발송한다. 실패 시 예외를 throw하여 컨트롤러가 오류 응답을 반환하게 한다.
     */
    public void sendTestEmail(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) return;
        if (smtpUsername == null || smtpUsername.isBlank()) {
            throw new IllegalStateException("SMTP 계정이 설정되지 않았습니다. 서버 관리자에게 문의하세요.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(smtpUsername != null ? smtpUsername : "");
            helper.setTo(recipientEmail);
            helper.setSubject("[WatchService] 이메일 알림 테스트");
            helper.setText("""
                    <div style="font-family:sans-serif;padding:24px;background:#f8fafc;">
                      <h2 style="color:#1e40af;">WatchService 이메일 알림 테스트</h2>
                      <p>이 메시지는 이메일 알림 설정이 정상적으로 동작함을 확인하는 테스트 메일입니다.</p>
                      <p style="color:#6b7280;font-size:13px;">DANGER 탐지 시 이 주소로 경보 메일이 발송됩니다.</p>
                    </div>
                    """, true);

            mailSender.send(message);
            log.info("[EmailNotificationService] 테스트 이메일 발송 완료 → {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("[EmailNotificationService] 테스트 이메일 발송 실패 → {}", recipientEmail, e);
            throw new RuntimeException("메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String buildHtmlBody(Notification n) {
        String createdAt = n.getCreatedAt() != null ? FMT.format(n.getCreatedAt()) : "-";
        String score = n.getAiScore() != null
                ? String.format("%.1f%%", n.getAiScore() * 100)
                : "-";
        String family = n.getTopFamily() != null ? n.getTopFamily() : "-";
        String detail = n.getAiDetail() != null
                ? n.getAiDetail().replace("\n", "<br>")
                : "-";
        String guidance = n.getGuidance() != null
                ? n.getGuidance().replace("\n", "<br>")
                : "-";

        String pathsHtml = "";
        if (n.getAffectedPaths() != null && !n.getAffectedPaths().isEmpty()) {
            StringBuilder sb = new StringBuilder("<ul style='margin:4px 0;padding-left:18px;'>");
            for (String p : n.getAffectedPaths()) {
                sb.append("<li style='font-family:monospace;font-size:12px;'>").append(p).append("</li>");
            }
            sb.append("</ul>");
            pathsHtml = sb.toString();
        }

        return """
                <div style="font-family:sans-serif;padding:24px;background:#f8fafc;">
                  <h2 style="color:#dc2626;">⚠ DANGER — 랜섬웨어 의심 활동 탐지</h2>
                  <p style="color:#374151;">WatchService가 랜섬웨어 의심 활동을 탐지했습니다. 즉시 확인하세요.</p>
                  <table style="border-collapse:collapse;width:100%;margin-top:16px;">
                    <tr><td style="padding:6px 12px;color:#6b7280;width:120px;">탐지 시각</td><td style="padding:6px 12px;">%s</td></tr>
                    <tr style="background:#fff5f5;"><td style="padding:6px 12px;color:#6b7280;">위험도</td><td style="padding:6px 12px;color:#dc2626;font-weight:bold;">DANGER (%s)</td></tr>
                    <tr><td style="padding:6px 12px;color:#6b7280;">패밀리</td><td style="padding:6px 12px;">%s</td></tr>
                    <tr style="background:#fff5f5;"><td style="padding:6px 12px;color:#6b7280;">영향 파일 수</td><td style="padding:6px 12px;">%d개</td></tr>
                  </table>
                  <div style="margin-top:16px;">
                    <strong style="color:#374151;">AI 분석:</strong><br>
                    <div style="margin-top:4px;padding:10px;background:#fff;border-left:3px solid #dc2626;color:#374151;">%s</div>
                  </div>
                  <div style="margin-top:16px;">
                    <strong style="color:#374151;">대응 방법:</strong><br>
                    <div style="margin-top:4px;padding:10px;background:#fff;border-left:3px solid #2563eb;color:#374151;">%s</div>
                  </div>
                  %s
                  <p style="margin-top:24px;font-size:12px;color:#9ca3af;">이 메일은 WatchService Agent가 자동 발송했습니다.</p>
                </div>
                """.formatted(createdAt, score, family, n.getAffectedFilesCount(), detail, guidance,
                pathsHtml.isEmpty() ? "" : "<div style='margin-top:12px;'><strong>영향 파일:</strong>" + pathsHtml + "</div>");
    }
}
