package com.orderlifecycle.ingress_service.UtilTests;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.orderlifecycle.ingress_service.utils.ScaleNormalizerUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScaleNormalizerUtil Unit Tests")
public class ScaleNormalizerUtilTest {

    private static final String SYMBOL = "AAPL";
    private static final int EXPECTED_SCALE = 2;

    private final ScaleNormalizerUtil scaleNormalizerUtil = new ScaleNormalizerUtil();

    @Test
    @DisplayName("Should return null safely without exception when input value is null")
    public void normalize_nullValue_returnsNull() {
        // Act
        BigDecimal result = scaleNormalizerUtil.normalize(null, SYMBOL);

        // Assert
        assertNull(result, "Null input must return null safely");
    }

    @ParameterizedTest(name = "Input: {0} -> Expected: {1}")
    @CsvSource({
        "10.50, 10.50",
        "10.5,  10.50",
        "10,    10.00",
        "0,     0.00",
        "0.1,   0.10"
    })
    @DisplayName("Should format numbers to exact scale of 2 decimal places when input scale is <= 2")
    public void normalize_validScale_normalizesToExpectedScale(String inputString, String expectedString) {
        // Arrange
        BigDecimal input = new BigDecimal(inputString);
        BigDecimal expected = new BigDecimal(expectedString);

        // Act
        BigDecimal actual = scaleNormalizerUtil.normalize(input, SYMBOL);

        // Assert
        assertEquals(expected, actual, "Normalized value must match expected decimal representation");
        assertEquals(EXPECTED_SCALE, actual.scale(), "Output scale must strictly be 2 decimal places");
    }

    @ParameterizedTest(name = "Excess scale input: {0}")
    @ValueSource(strings = { "10.555", "0.001", "123.4567" })
    @DisplayName("Should throw ResponseStatusException (400 Bad Request) when scale exceeds 2 decimal places")
    public void normalize_exceededScale_throwsResponseStatusException(String inputString) {
        // Arrange
        BigDecimal input = new BigDecimal(inputString);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> scaleNormalizerUtil.normalize(input, SYMBOL),
                "Should throw ResponseStatusException when scale > expectedScale"
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains(SYMBOL), "Exception message must identify the affected asset symbol");
        assertTrue(exception.getReason().contains(String.valueOf(EXPECTED_SCALE)), "Exception message must report the expected scale");
    }
}
