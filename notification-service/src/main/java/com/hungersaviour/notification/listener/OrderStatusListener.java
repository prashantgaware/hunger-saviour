package com.hungersaviour.notification.listener;

import com.hungersaviour.notification.dto.OrderStatusEvent;
import com.hungersaviour.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderStatusListener {

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = "order.status.queue")
    public void handleOrderStatusUpdate(OrderStatusEvent event) {
        log.info("Received order status update: Order ID={}, Status={}", event.getOrderId(), event.getStatus());
        
        // Send notification to customer
        if (event.getUserEmail() != null && !event.getUserEmail().isEmpty()) {
            String customerSubject = "Order #" + event.getOrderId() + " - " + getStatusDisplayName(event.getStatus());
            String customerBody = buildCustomerEmailBody(event);
            emailService.sendOrderStatusEmail(event.getUserEmail(), customerSubject, customerBody);
            log.info("Customer notification sent to: {}", event.getUserEmail());
        }
        
        // Send notification to restaurant owner
        if (event.getRestaurantEmail() != null && !event.getRestaurantEmail().isEmpty()) {
            String restaurantSubject = "New Order #" + event.getOrderId() + " - " + getStatusDisplayName(event.getStatus());
            String restaurantBody = buildRestaurantEmailBody(event);
            emailService.sendOrderStatusEmail(event.getRestaurantEmail(), restaurantSubject, restaurantBody);
            log.info("Restaurant notification sent to: {}", event.getRestaurantEmail());
        }
    }
    
    private String getStatusDisplayName(String status) {
        switch (status) {
            case "PENDING": return "Order Received";
            case "PAYMENT_PROCESSING": return "Processing Payment";
            case "CONFIRMED": return "Order Confirmed";
            case "PREPARING": return "Preparing Your Order";
            case "OUT_FOR_DELIVERY": return "Out for Delivery";
            case "DELIVERED": return "Delivered";
            case "CANCELLED": return "Order Cancelled";
            case "PAYMENT_FAILED": return "Payment Failed";
            default: return "Status Update";
        }
    }

    private String buildCustomerEmailBody(OrderStatusEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Customer,\n\n");
        
        switch (event.getStatus()) {
            case "PENDING":
                body.append("We have received your order!\n\n");
                break;
            case "CONFIRMED":
                body.append("Great news! Your order has been confirmed and payment was successful.\n\n");
                break;
            case "PREPARING":
                body.append("Your order is being prepared by the restaurant.\n\n");
                break;
            case "OUT_FOR_DELIVERY":
                body.append("Your order is on its way to you!\n\n");
                break;
            case "DELIVERED":
                body.append("Your order has been delivered. Enjoy your meal!\n\n");
                break;
            case "CANCELLED":
                body.append("Your order has been cancelled.\n\n");
                break;
            case "PAYMENT_FAILED":
                body.append("Unfortunately, your payment could not be processed. Please try again.\n\n");
                break;
            default:
                body.append("Your order status has been updated.\n\n");
        }
        
        body.append("Order Details:\n");
        body.append("Order ID: #").append(event.getOrderId()).append("\n");
        body.append("Restaurant: ").append(event.getRestaurantName()).append("\n");
        body.append("Status: ").append(getStatusDisplayName(event.getStatus())).append("\n");
        body.append("Total Amount: $").append(String.format("%.2f", event.getTotalAmount())).append("\n");
        
        if (event.getDeliveryAddress() != null) {
            body.append("Delivery Address: ").append(event.getDeliveryAddress()).append("\n");
        }
        
        body.append("\nThank you for choosing Hunger Saviour!\n\n");
        body.append("Best regards,\n");
        body.append("Hunger Saviour Team");
        
        return body.toString();
    }

    private String buildRestaurantEmailBody(OrderStatusEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Restaurant Partner,\n\n");
        
        switch (event.getStatus()) {
            case "CONFIRMED":
            case "PREPARING":
                body.append("You have a new order to prepare!\n\n");
                break;
            case "CANCELLED":
                body.append("An order has been cancelled.\n\n");
                break;
            default:
                body.append("Order status update notification.\n\n");
        }
        
        body.append("Order Details:\n");
        body.append("Order ID: #").append(event.getOrderId()).append("\n");
        body.append("Status: ").append(getStatusDisplayName(event.getStatus())).append("\n");
        body.append("Total Amount: $").append(String.format("%.2f", event.getTotalAmount())).append("\n");
        body.append("Delivery Address: ").append(event.getDeliveryAddress()).append("\n\n");
        
        if ("PREPARING".equals(event.getStatus())) {
            body.append("⚠️ Please start preparing this order immediately.\n\n");
        }
        
        body.append("Please log in to your dashboard to view full order details.\n\n");
        body.append("Best regards,\n");
        body.append("Hunger Saviour Team");
        
        return body.toString();
    }
}
