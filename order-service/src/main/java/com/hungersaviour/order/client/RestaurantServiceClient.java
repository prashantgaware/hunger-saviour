package com.hungersaviour.order.client;

import com.hungersaviour.order.dto.RestaurantResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@Slf4j
public class RestaurantServiceClient {

    private final WebClient webClient;

    @Value("${services.restaurant.url:http://restaurant-service:8082}")
    private String restaurantServiceUrl;

    @Autowired
    public RestaurantServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public RestaurantResponse getRestaurantById(Long restaurantId) {
        try {
            log.info("Fetching restaurant details for ID: {}", restaurantId);
            
            return webClient.get()
                    .uri(restaurantServiceUrl + "/api/restaurants/" + restaurantId)
                    .retrieve()
                    .bodyToMono(RestaurantResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("Error fetching restaurant details: {}", e.getMessage());
            throw new RuntimeException("Restaurant service unavailable: " + e.getMessage());
        }
    }
}
