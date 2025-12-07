package com.pbl6.cinemate.shared.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String replyTo;
}
