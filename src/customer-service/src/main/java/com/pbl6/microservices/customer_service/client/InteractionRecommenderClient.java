package com.pbl6.microservices.customer_service.client;

import com.pbl6.microservices.customer_service.client.dto.FavoriteEventRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "interaction-recommender-service", url = "${interaction.recommender.service.url:http://interaction-recommender-service:8000}")
public interface InteractionRecommenderClient {

    @PostMapping("/api/v1/events/favorite")
    void trackFavoriteEvent(@RequestBody FavoriteEventRequest request);

}
