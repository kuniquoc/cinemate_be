package com.pbl6.cinemate.shared.email.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Locale;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TemplateEmailRequest extends EmailRequest {
    private String templateName;
    private Map<String, Object> variables;
    private Locale locale;
}
