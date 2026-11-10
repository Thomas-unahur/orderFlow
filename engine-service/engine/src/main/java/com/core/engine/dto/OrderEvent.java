package com.core.engine.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.core.engine.dto.enums.OrderType;
import com.core.engine.dto.enums.Side;
import com.core.engine.dto.enums.OrderStatus;

public record OrderEvent(
                @NotNull(message = "Order id cannot be null") UUID orderId,
                @NotBlank(message = "User id cannot be null") String userId,
                @NotBlank(message = "Symbol cannot be null") String symbol,
                @NotNull(message = "Side cannot be null") Side side,
                @NotNull(message = "Order type cannot be null") OrderType type,
                @NotNull(message = "Quantity cannot be null") @Positive(message = "Quantity must be positive and greater than cero") BigDecimal quantity,
                @Positive(message = "Price must be positive and greater than cero") BigDecimal price,
                @NotNull(message = "Status cannot be null") OrderStatus status,
                @NotNull(message = "Timestamp cannot be null") long timestamp) {
}
