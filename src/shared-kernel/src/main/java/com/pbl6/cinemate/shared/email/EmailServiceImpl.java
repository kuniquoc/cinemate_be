package com.pbl6.cinemate.shared.email;

import com.pbl6.cinemate.shared.config.AppProperties;
import com.pbl6.cinemate.shared.email.dto.TemplateEmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties appProperties;

    @Override
    @Async("emailTaskExecutor")
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getEmail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getEmail().getFrom(), appProperties.getEmail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTemplateEmail(TemplateEmailRequest request) {
        try {
            // Process template
            Context context = new Context();
            Locale locale = request.getLocale() != null ? request.getLocale() : Locale.ENGLISH;
            context.setLocale(locale);

            if (request.getVariables() != null) {
                for (Map.Entry<String, Object> entry : request.getVariables().entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            String htmlContent = templateEngine.process(request.getTemplateName(), context);

            // Create message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getEmail().getFrom(), appProperties.getEmail().getFromName());
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(htmlContent, true);

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            if (request.getReplyTo() != null) {
                helper.setReplyTo(request.getReplyTo());
            }

            mailSender.send(message);
            log.info("Template email sent successfully to: {} using template: {}", request.getTo(),
                    request.getTemplateName());
        } catch (Exception e) {
            log.error("Failed to send template email to: {}. Error: {}", request.getTo(), e.getMessage(), e);
        }
    }
}
