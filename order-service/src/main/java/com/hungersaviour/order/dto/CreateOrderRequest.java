package com.hungersaviour.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    private Long userId;
    private Long restaurantId;
    private String deliveryAddress;
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        private Long menuItemId;
        private String menuItemName;
        private Integer quantity;
        private BigDecimal price;
    }
}
