package com.hungersaviour.order.client;

import com.hungersaviour.order.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    @Value("${services.user.url:http://user-service:8081}")
    private String userServiceUrl;

    @Autowired
    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public UserResponse getUserById(Long userId) {
        try {
            log.info("Fetching user details for ID: {}", userId);
            
            return webClient.get()
                    .uri(userServiceUrl + "/api/users/" + userId)
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("Error fetching user details: {}", e.getMessage());
            throw new RuntimeException("User service unavailable: " + e.getMessage());
        }
    }
}
