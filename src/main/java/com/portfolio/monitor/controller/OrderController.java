package com.portfolio.monitor.controller;

import com.portfolio.monitor.dto.OrderRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.UUID;

@RestController
public class OrderController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/dashboard.html");
    }


    @PostMapping("/api/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "COMPLETED",
                "product", request.getProductName(),
                "quantity", request.getQuantity(),
                "totalPrice", request.getPrice() * request.getQuantity()
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
