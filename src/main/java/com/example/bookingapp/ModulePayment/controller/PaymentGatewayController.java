package com.example.bookingapp.ModulePayment.controller;

import com.example.bookingapp.ModulePayment.dto.RazorpayOrderRequest;
import com.example.bookingapp.ModulePayment.dto.RazorpayVerifyRequest;
import com.example.bookingapp.ModulePayment.dto.StripeCheckoutRequest;
import com.example.bookingapp.ModulePayment.service.RazorpayService;
import com.example.bookingapp.ModulePayment.service.StripeService;
import com.example.bookingapp.ModuleSubscription.entity.SubscriptionPlan;
import com.example.bookingapp.ModuleSubscription.entity.UserSubscription;
import com.example.bookingapp.ModuleSubscription.repository.SubscriptionPlanRepository;
import com.example.bookingapp.ModuleSubscription.service.UserSubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentGatewayController {

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private UserSubscriptionService userSubscriptionService;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    // ==================== RAZORPAY ENDPOINTS ====================

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody RazorpayOrderRequest request) {
        try {
            Map<String, Object> order = razorpayService.createOrder(
                    request.getAmount(),
                    request.getCurrency()
            );
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create Razorpay order: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verifyRazorpayPayment(@RequestBody RazorpayVerifyRequest request) {
        try {
            boolean isValid = razorpayService.verifyPaymentSignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );

            if (!isValid) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid payment signature");
                return ResponseEntity.badRequest().body(error);
            }

            // Create subscription after successful payment
            UserSubscription subscription = userSubscriptionService.createSubscription(
                    request.getUserId(),
                    request.getPlanId(),
                    "razorpay",
                    request.getRazorpayPaymentId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified successfully");
            response.put("subscription", subscription);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Payment verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== STRIPE ENDPOINTS ====================

    @PostMapping("/stripe/create-checkout-session")
    public ResponseEntity<?> createStripeCheckoutSession(@RequestBody StripeCheckoutRequest request) {
        try {
            // Get plan details
            SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            Map<String, String> session = stripeService.createCheckoutSession(
                    plan.getPlanName(),
                    plan.getDescription(),
                    plan.getPrice().doubleValue(),
                    request.getPlanId(),
                    request.getUserId(),
                    request.getSuccessUrl(),
                    request.getCancelUrl()
            );

            return ResponseEntity.ok(session);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create Stripe checkout session: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<?> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = stripeService.constructWebhookEvent(payload, sigHeader);

            // Handle the event
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));

                Map<String, String> metadata = session.getMetadata();
                Long userId = Long.parseLong(metadata.get("userId"));
                Long planId = Long.parseLong(metadata.get("planId"));

                // Create subscription
                userSubscriptionService.createSubscription(
                        userId,
                        planId,
                        "stripe",
                        session.getPaymentIntent()
                );
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    @GetMapping("/test")
    public ResponseEntity<?> testPaymentGateways() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Payment gateways are configured");
        response.put("razorpay", "Available");
        response.put("stripe", "Available");
        return ResponseEntity.ok(response);
    }
}
