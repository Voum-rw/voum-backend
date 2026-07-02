package com.voum.modules.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends an OTP verification email asynchronously so it doesn't block the request thread.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Voum");
            helper.setTo(toEmail);
            helper.setSubject("Your Voum verification code");
            helper.setText(buildOtpEmailHtml(otpCode), true); // true = HTML

            mailSender.send(message);
            log.info("OTP email sent successfully to '{}'", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Log but don't throw — OTP is already in Redis, user can retry
            log.error("Failed to send OTP email to '{}': {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                             background: #f9fafb; margin: 0; padding: 40px 0;">
                  <div style="max-width: 480px; margin: 0 auto; background: white;
                              border-radius: 16px; overflow: hidden;
                              box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
                    <div style="background: #DC2626; padding: 32px; text-align: center;">
                      <h1 style="color: white; font-size: 32px; font-weight: 900;
                                 letter-spacing: -1px; margin: 0;">voum</h1>
                    </div>
                    <div style="padding: 40px 32px;">
                      <h2 style="color: #111827; font-size: 22px; font-weight: 700; margin: 0 0 12px;">
                        Your verification code
                      </h2>
                      <p style="color: #6b7280; font-size: 15px; line-height: 1.6; margin: 0 0 32px;">
                        Use the code below to verify your identity. It expires in <strong>5 minutes</strong>.
                      </p>
                      <div style="background: #f3f4f6; border-radius: 12px; padding: 24px;
                                  text-align: center; margin-bottom: 32px;">
                        <span style="font-size: 40px; font-weight: 900; letter-spacing: 12px;
                                     color: #DC2626; font-family: monospace;">%s</span>
                      </div>
                      <p style="color: #9ca3af; font-size: 13px; line-height: 1.5; margin: 0;">
                        If you didn't request this code, you can safely ignore this email.
                        Someone may have entered your email address by mistake.
                      </p>
                    </div>
                    <div style="background: #f9fafb; padding: 20px 32px; text-align: center;
                                border-top: 1px solid #f3f4f6;">
                      <p style="color: #9ca3af; font-size: 12px; margin: 0;">
                        © 2025 Voum · Rwanda's direct moto-taxi platform
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otpCode);
    }
}
