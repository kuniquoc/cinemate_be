package com.pbl6.cinemate.shared.email;

import com.pbl6.cinemate.shared.email.dto.TemplateEmailRequest;

public interface EmailService {
    
    /**
     * Send a simple text email
     */
    void sendSimpleEmail(String to, String subject, String body);
    
    /**
     * Send an HTML email
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
    
    /**
     * Send an email using a Thymeleaf template
     */
    void sendTemplateEmail(TemplateEmailRequest request);
}
