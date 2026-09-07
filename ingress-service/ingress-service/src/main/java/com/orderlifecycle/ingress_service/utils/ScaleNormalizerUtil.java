package com.orderlifecycle.ingress_service.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ScaleNormalizerUtil {

    public BigDecimal normalize(BigDecimal value, String symbol) {
        if (value == null) {
            return null;
        }

        int expectedScale = 2;

        if (value.scale() > expectedScale) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Asset " + symbol + " only accepts " + expectedScale + " decimal places");
        }

        return value.setScale(expectedScale, RoundingMode.HALF_UP);
    }
}