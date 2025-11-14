package com.hungersaviour.payment.service;

import com.hungersaviour.payment.dto.PaymentRequest;
import com.hungersaviour.payment.dto.PaymentResponse;
import com.hungersaviour.payment.exception.PaymentProcessingException;
import com.hungersaviour.payment.exception.ResourceNotFoundException;
import com.hungersaviour.payment.model.Payment;
import com.hungersaviour.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Create payment intent with Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue()) // Convert to cents
                    .setCurrency(request.getCurrency())
                    .setPaymentMethod(request.getPaymentMethodId())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Save payment record
            Payment payment = new Payment();
            payment.setOrderId(request.getOrderId());
            payment.setUserId(request.getUserId());
            payment.setAmount(request.getAmount());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setPaymentMethod("CARD");

            if ("succeeded".equals(intent.getStatus())) {
                payment.setStatus("SUCCESS");
                // Charge ID can be retrieved separately if needed via Stripe API
            } else {
                payment.setStatus("FAILED");
            }

            payment = paymentRepository.save(payment);

            return new PaymentResponse(
                    payment.getId(),
                    payment.getStatus(),
                    "Payment processed successfully",
                    intent.getId()
            );

        } catch (StripeException e) {
            // Save failed payment
            Payment payment = new Payment();
            payment.setOrderId(request.getOrderId());
            payment.setUserId(request.getUserId());
            payment.setAmount(request.getAmount());
            payment.setStatus("FAILED");
            payment.setPaymentMethod("CARD");
            payment = paymentRepository.save(payment);

            return new PaymentResponse(
                    payment.getId(),
                    "FAILED",
                    "Payment failed: " + e.getMessage(),
                    null
            );
        }
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
    }

    public List<Payment> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new PaymentProcessingException("Can only refund successful payments");
        }

        try {
            // Create refund in Stripe
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(
                    com.stripe.param.RefundCreateParams.builder()
                            .setPaymentIntent(payment.getStripePaymentIntentId())
                            .build()
            );

            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);

            return new PaymentResponse(
                    payment.getId(),
                    "REFUNDED",
                    "Payment refunded successfully",
                    payment.getStripePaymentIntentId()
            );

        } catch (StripeException e) {
            throw new PaymentProcessingException("Refund failed: " + e.getMessage());
        }
    }
}
