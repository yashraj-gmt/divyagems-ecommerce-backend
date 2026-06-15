package com.divyagems.ecommerce.order.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures a singleton RazorpayClient bean from application properties.
 * The client is thread-safe and reused across the application lifecycle.
 */
@Slf4j
@Configuration
public class RazorpayConfig {

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        log.info("Initializing Razorpay client with key-id: {}...", keyId.substring(0, Math.min(8, keyId.length())));
        return new RazorpayClient(keyId, keySecret);
    }
}
