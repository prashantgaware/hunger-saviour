package com.hungersaviour.order.client;

import com.hungersaviour.order.dto.PaymentRequest;
import com.hungersaviour.order.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class PaymentServiceClient {

    private final WebClient webClient;

    @Value("${services.payment.url:http://payment-service:8084}")
    private String paymentServiceUrl;

    @Autowired
    public PaymentServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            log.info("Calling payment service to process payment for order: {}", request.getOrderId());
            
            return webClient.post()
                    .uri(paymentServiceUrl + "/api/payments")
                    .body(Mono.just(request), PaymentRequest.class)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.error("Error calling payment service: {}", e.getMessage());
            throw new RuntimeException("Payment service unavailable: " + e.getMessage());
        }
    }
}
