package com.pbl6.cinemate.payment_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VNPayConfig {
    
    private String url;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private String ipnUrl;
}
