package com.example.bookingapp.ModulePayment.service;

import com.example.bookingapp.ModuleAdmin.service.AdminSettingsService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private AdminSettingsService adminSettingsService;

    public Map<String, String> createCheckoutSession(
        String planName,
        String planDescription,
        Double amount,
        Long planId,
        Long userId,
        String successUrl,
        String cancelUrl
    ) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        // Get configured currency
        String currency = adminSettingsService.getCurrency().toLowerCase();

        // Validate minimum amount based on currency
        validateMinimumAmount(amount, currency);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(currency)
                                        .setUnitAmount((long) (amount * 100))
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(planName)
                                                .setDescription(planDescription)
                                                .build()
                                        )
                                        .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("planId", String.valueOf(planId))
                .build();

        Session session = Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("sessionUrl", session.getUrl());

        return response;
    }

    private void validateMinimumAmount(Double amount, String currency) {
        Map<String, Double> minimumAmounts = new HashMap<>();
        minimumAmounts.put("usd", 0.50);
        minimumAmounts.put("eur", 0.50);
        minimumAmounts.put("gbp", 0.30);
        minimumAmounts.put("inr", 50.00);
        minimumAmounts.put("aud", 0.50);
        minimumAmounts.put("cad", 0.50);
        minimumAmounts.put("sgd", 0.50);
        
        Double minAmount = minimumAmounts.getOrDefault(currency.toLowerCase(), 0.50);
        
        if (amount < minAmount) {
            throw new IllegalArgumentException(
                String.format("Amount %.2f %s is below Stripe's minimum of %.2f %s", 
                    amount, currency.toUpperCase(), minAmount, currency.toUpperCase())
            );
        }
    }

    public Event constructWebhookEvent(String payload, String sigHeader) throws Exception {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }

    public Map<String, String> getSessionMetadata(String sessionId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        Session session = Session.retrieve(sessionId);
        return session.getMetadata();
    }
}
