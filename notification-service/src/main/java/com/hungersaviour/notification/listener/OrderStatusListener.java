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
        log.info("Received order status update: {}", event);
        
        String subject = "Order #" + event.getOrderId() + " Status Update";
        String body = buildEmailBody(event);
        
        emailService.sendOrderStatusEmail(event.getUserEmail(), subject, body);
    }

    private String buildEmailBody(OrderStatusEvent event) {
        return String.format(
            "Dear Customer,\n\n" +
            "Your order #%d from %s has been updated.\n\n" +
            "Status: %s\n" +
            "Total Amount: $%.2f\n\n" +
            "Thank you for using Hunger Saviour!\n\n" +
            "Best regards,\n" +
            "Hunger Saviour Team",
            event.getOrderId(),
            event.getRestaurantName(),
            event.getStatus(),
            event.getTotalAmount()
        );
    }
}
