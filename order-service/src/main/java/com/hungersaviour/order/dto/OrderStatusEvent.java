package com.hungersaviour.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantEmail;
    private String status;
    private Double totalAmount;
    private String deliveryAddress;
}
