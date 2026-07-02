package com.voum.modules.notification;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    /**
     * Sends an OTP verification email asynchronously via Resend's HTTPS API.
     * Falls back to logging if the Resend API key is not configured.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        if (resendApiKey.isBlank()) {
            log.warn("[EMAIL FALLBACK] Resend API key not configured. OTP for '{}' is: {}", toEmail, otpCode);
            return;
        }

        try {
            Resend resend = new Resend(resendApiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Voum <" + fromEmail + ">")
                    .to(toEmail)
                    .subject("Your Voum verification code")
                    .html(buildOtpEmailHtml(otpCode))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("OTP email sent successfully via Resend API. Response ID: '{}'", response.getId());

        } catch (Exception e) {
            log.error("Failed to send OTP email via Resend to '{}': {}", toEmail, e.getMessage());
            log.warn("[EMAIL FALLBACK] Resend failed. OTP for '{}' is: {}", toEmail, otpCode);
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
