package com.hungersaviour.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String status;
    private String restaurantName;
    private Double totalAmount;
}
