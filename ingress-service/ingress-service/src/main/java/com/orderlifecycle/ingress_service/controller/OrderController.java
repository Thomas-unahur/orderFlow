package com.orderlifecycle.ingress_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.service.OrderIngressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderIngressService orderIngressService;

    @PostMapping
    public ResponseEntity<String> receiveOrder(@Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal String userId) {
        orderIngressService.processOrder(orderRequest, userId);
        return ResponseEntity.accepted().body("Order in progress");
    }
}
