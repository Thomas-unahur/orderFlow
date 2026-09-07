package com.orderlifecycle.ingress_service.dto;

import java.math.BigDecimal;

import com.orderlifecycle.ingress_service.dto.enums.OrderType;
import com.orderlifecycle.ingress_service.dto.enums.Side;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotBlank(message = "Symbol is required") String symbol,
        @NotNull(message = "Side (BUY/SELL) is required") Side side,
        @NotNull(message = "Type (LIMIT/MARKET) is required") OrderType type,
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive and greater than cero") BigDecimal quantity,
        @Positive(message = "Price must be positive") BigDecimal price) {

    public OrderRequest {
        if (symbol != null) {
            symbol = symbol.trim().toUpperCase();
        }
    }

    @AssertTrue(message = "Price is required and must be greater than zero for LIMIT orders")
    public boolean isPriceValidForOrderType() {
        if (type == OrderType.LIMIT) {
            return price != null && price.compareTo(BigDecimal.ZERO) > 0;
        }

        return true;
    }
}