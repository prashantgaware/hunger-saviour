package com.hungersaviour.order.service;

import com.hungersaviour.order.client.PaymentServiceClient;
import com.hungersaviour.order.client.RestaurantServiceClient;
import com.hungersaviour.order.client.UserServiceClient;
import com.hungersaviour.order.dto.*;
import com.hungersaviour.order.model.Order;
import com.hungersaviour.order.model.OrderItem;
import com.hungersaviour.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private RestaurantServiceClient restaurantServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_ROUTING_KEY = "order.status";

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}, restaurant: {}", request.getUserId(), request.getRestaurantId());
        
        // Step 1: Fetch user details
        UserResponse user = userServiceClient.getUserById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("User not found: " + request.getUserId());
        }
        log.info("User found: {}", user.getEmail());
        
        // Step 2: Fetch restaurant details
        RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(request.getRestaurantId());
        if (restaurant == null) {
            throw new RuntimeException("Restaurant not found: " + request.getRestaurantId());
        }
        log.info("Restaurant found: {}", restaurant.getName());
        
        // Step 3: Create order with PENDING status
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setRestaurantId(request.getRestaurantId());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setStatus("PENDING");

        final Order finalOrder = order; // Make it final for lambda
        List<OrderItem> orderItems = request.getItems().stream().map(itemDto -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(finalOrder);
            orderItem.setMenuItemId(itemDto.getMenuItemId());
            orderItem.setMenuItemName(itemDto.getMenuItemName());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(itemDto.getPrice());
            orderItem.setSubtotal(itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            return orderItem;
        }).collect(Collectors.toList());

        order.setOrderItems(orderItems);

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // Save order first to get ID
        order = orderRepository.save(order);
        log.info("Order created with ID: {} and status: PENDING", order.getId());
        
        // Publish order creation event to notify customer
        publishOrderEvent(order, user, restaurant, "ORDER_PLACED");
        
        // Step 4: Process payment if payment method is provided
        if (request.getPaymentMethodId() != null && !request.getPaymentMethodId().isEmpty()) {
            try {
                order.setStatus("PAYMENT_PROCESSING");
                order = orderRepository.save(order);
                log.info("Order status updated to: PAYMENT_PROCESSING");
                
                PaymentRequest paymentRequest = new PaymentRequest(
                    order.getId(),
                    request.getUserId(),
                    totalAmount,
                    request.getPaymentMethodId(),
                    "usd"
                );
                
                PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);
                
                if ("SUCCESS".equals(paymentResponse.getStatus())) {
                    // Payment successful
                    order.setPaymentId(paymentResponse.getPaymentId());
                    order.setStatus("CONFIRMED");
                    order = orderRepository.save(order);
                    log.info("Payment successful. Order status updated to: CONFIRMED");
                    
                    // Publish confirmation event - notify both customer and restaurant
                    publishOrderEvent(order, user, restaurant, "ORDER_CONFIRMED");
                    
                    // Automatically move to PREPARING status
                    order.setStatus("PREPARING");
                    order = orderRepository.save(order);
                    log.info("Order status updated to: PREPARING");
                    
                    // Notify restaurant to prepare order
                    publishOrderEvent(order, user, restaurant, "PREPARING");
                    
                } else {
                    // Payment failed
                    order.setStatus("PAYMENT_FAILED");
                    order = orderRepository.save(order);
                    log.error("Payment failed for order: {}", order.getId());
                    
                    publishOrderEvent(order, user, restaurant, "PAYMENT_FAILED");
                    throw new RuntimeException("Payment failed: " + paymentResponse.getMessage());
                }
            } catch (Exception e) {
                log.error("Payment processing error: {}", e.getMessage());
                order.setStatus("PAYMENT_FAILED");
                orderRepository.save(order);
                publishOrderEvent(order, user, restaurant, "PAYMENT_FAILED");
                throw new RuntimeException("Payment processing failed: " + e.getMessage());
            }
        }

        return order;
    }
    
    private void publishOrderEvent(Order order, UserResponse user, RestaurantResponse restaurant, String eventType) {
        try {
            OrderStatusEvent event = new OrderStatusEvent();
            event.setOrderId(order.getId());
            event.setUserId(order.getUserId());
            event.setUserEmail(user.getEmail());
            event.setRestaurantId(order.getRestaurantId());
            event.setRestaurantName(restaurant.getName());
            event.setRestaurantEmail(restaurant.getOwnerEmail());
            event.setStatus(order.getStatus());
            event.setTotalAmount(order.getTotalAmount().doubleValue());
            event.setDeliveryAddress(order.getDeliveryAddress());
            
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_ROUTING_KEY, event);
            log.info("Published {} event for order: {}", eventType, order.getId());
        } catch (Exception e) {
            log.error("Failed to publish order event: {}", e.getMessage());
            // Don't fail the order if notification fails
        }
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId);
    }

    public Order updateOrderStatus(Long id, String status) {
        log.info("Updating order {} status to: {}", id, status);
        Order order = getOrderById(id);
        order.setStatus(status);
        order = orderRepository.save(order);
        
        // Fetch user and restaurant details for notification
        try {
            UserResponse user = userServiceClient.getUserById(order.getUserId());
            RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
            publishOrderEvent(order, user, restaurant, "STATUS_UPDATE");
        } catch (Exception e) {
            log.error("Failed to publish status update event: {}", e.getMessage());
        }
        
        return order;
    }

    public void cancelOrder(Long id) {
        log.info("Cancelling order: {}", id);
        Order order = getOrderById(id);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        
        // Notify about cancellation
        try {
            UserResponse user = userServiceClient.getUserById(order.getUserId());
            RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
            publishOrderEvent(order, user, restaurant, "ORDER_CANCELLED");
        } catch (Exception e) {
            log.error("Failed to publish cancellation event: {}", e.getMessage());
        }
    }
}
