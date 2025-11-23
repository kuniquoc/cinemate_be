package com.pbl6.microservices.customer_service.payload.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageMeta {
    @JsonProperty("limit")
    int limit;

    @JsonProperty("current_page")
    int currentPage;

    @JsonProperty("total_page")
    int totalPage;
}
