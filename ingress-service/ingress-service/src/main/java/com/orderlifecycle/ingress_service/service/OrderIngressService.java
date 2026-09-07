package com.orderlifecycle.ingress_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.orderlifecycle.ingress_service.dto.OrderEvent;
import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.dto.enums.OrderStatus;
import com.orderlifecycle.ingress_service.utils.ScaleNormalizerUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIngressService {
    private final ScaleNormalizerUtil scaleNormalizerUtil;

    public void processOrder(OrderRequest orderRequest, String userId) {
        var normalizedPrice = scaleNormalizerUtil.normalize(orderRequest.price(), orderRequest.symbol());
        var normalizedQuantity = scaleNormalizerUtil.normalize(orderRequest.quantity(), orderRequest.symbol());

        UUID newOrderId = UUID.randomUUID();

        Instant now = Instant.now();

        long timestampNanos = (now.getEpochSecond() * 1_000_000_000L) + now.getNano();

        OrderEvent orderEvent = new OrderEvent(
                newOrderId,
                userId,
                orderRequest.symbol(),
                orderRequest.side(),
                orderRequest.type(),
                normalizedQuantity,
                normalizedPrice,
                OrderStatus.PENDING,
                timestampNanos);

        log.info("New order event created: {}", orderEvent);
    }
}
