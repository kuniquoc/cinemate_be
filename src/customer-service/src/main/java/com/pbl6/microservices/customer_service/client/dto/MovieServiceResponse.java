package com.pbl6.microservices.customer_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for movie-service responses that match their ResponseData structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieServiceResponse {
    private String status;
    private MovieDetailResponse data;
    private String message;
    private String path;
    private String method;
}
