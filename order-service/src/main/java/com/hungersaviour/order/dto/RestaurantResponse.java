package com.hungersaviour.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {
    private Long id;
    private String name;
    private String address;
    private String cuisine;
    private String description;
    private String phoneNumber;
    private Long ownerId;
    private String ownerEmail;
    private Boolean isActive;
}
