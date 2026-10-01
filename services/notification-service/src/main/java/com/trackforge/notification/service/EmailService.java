package com.trackforge.notification.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${sendgrid.api-key:}")
    private String sendGridApiKey;

    @Value("${from.email:noreply@trackforge.io}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String username) {
        send(toEmail, "Welcome to BugTracker 🐛",
                buildHtmlEmail("Welcome, " + username + "!",
                        "Your account has been created. Start tracking bugs now."));
    }

    public void sendBugAssignedEmail(String toEmail, String bugNumber, String bugTitle, String projectKey) {
        send(toEmail, "[" + projectKey + "] Bug assigned to you: " + bugNumber,
                buildHtmlEmail("You've been assigned a bug",
                        "<b>" + bugNumber + "</b>: " + bugTitle +
                                "<br><br>Log in to view details and start working on it."));
    }

    public void sendStatusChangedEmail(String toEmail, String bugNumber, String oldStatus, String newStatus) {
        send(toEmail, "[BugTracker] Status update: " + bugNumber,
                buildHtmlEmail("Bug status changed",
                        bugNumber + " status changed from <b>" + oldStatus + "</b> to <b>" + newStatus + "</b>."));
    }

    public void sendCommentNotificationEmail(String toEmail, String bugNumber, String commenter, String preview) {
        send(toEmail, "[BugTracker] New comment on " + bugNumber,
                buildHtmlEmail("New comment by " + commenter,
                        "\"" + preview + "\"<br><br>Log in to view the full thread."));
    }

    private void send(String toEmail, String subject, String htmlContent) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.info("[EMAIL SKIPPED - no API key] To: {} | Subject: {}", toEmail, subject);
            return;
        }
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("Email sent to {} - status {}", toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtmlEmail(String heading, String body) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Inter,sans-serif;background:#f8fafc;padding:24px">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;padding:32px;border:1px solid #e2e8f0">
                <div style="display:flex;align-items:center;gap:8px;margin-bottom:24px">
                  <span style="font-size:20px">🐛</span>
                  <span style="font-size:18px;font-weight:600;color:#1e293b">BugTracker</span>
                </div>
                <h2 style="color:#1e293b;margin:0 0 16px">%s</h2>
                <p style="color:#475569;line-height:1.6;margin:0 0 24px">%s</p>
                <a href="http://localhost:5173" style="background:#4f46e5;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;font-weight:500">
                  Open BugTracker →
                </a>
              </div>
            </body>
            </html>
            """.formatted(heading, body);
    }
}