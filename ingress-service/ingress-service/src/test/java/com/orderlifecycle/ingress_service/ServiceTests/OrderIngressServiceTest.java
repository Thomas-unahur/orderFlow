package com.orderlifecycle.ingress_service.ServiceTests;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.dto.enums.OrderType;
import com.orderlifecycle.ingress_service.dto.enums.Side;
import com.orderlifecycle.ingress_service.service.OrderIngressService;
import com.orderlifecycle.ingress_service.utils.ScaleNormalizerUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderIngressService Business Logic Tests")
public class OrderIngressServiceTest {

    private static final String DEFAULT_USER_ID = "usr-test-123";
    private static final String DEFAULT_SYMBOL = "MSFT";
    private static final BigDecimal RAW_QUANTITY = new BigDecimal("1.5");
    private static final BigDecimal NORMALIZED_QUANTITY = new BigDecimal("1.50");
    private static final BigDecimal RAW_PRICE = new BigDecimal("5000.0");
    private static final BigDecimal NORMALIZED_PRICE = new BigDecimal("5000.00");

    @Mock
    private ScaleNormalizerUtil scaleNormalizerUtil;

    @InjectMocks
    private OrderIngressService orderIngressService;

    @Test
    @DisplayName("Should normalize both price and quantity via ScaleNormalizerUtil for valid LIMIT orders")
    public void processOrder_validLimitOrder_normalizesPriceAndQuantity() {
        // Arrange
        OrderRequest request = new OrderRequest(
                DEFAULT_SYMBOL,
                Side.BUY,
                OrderType.LIMIT,
                RAW_QUANTITY,
                RAW_PRICE
        );

        when(scaleNormalizerUtil.normalize(RAW_PRICE, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_PRICE);
        when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

        // Act
        orderIngressService.processOrder(request, DEFAULT_USER_ID);

        // Assert
        verify(scaleNormalizerUtil).normalize(RAW_PRICE, DEFAULT_SYMBOL);
        verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
        verifyNoMoreInteractions(scaleNormalizerUtil);
    }

    @Test
    @DisplayName("Should normalize quantity and pass null price for MARKET orders without a defined price")
    public void processOrder_marketOrderWithNullPrice_normalizesNullPriceAndQuantity() {
        // Arrange
        OrderRequest request = new OrderRequest(
                DEFAULT_SYMBOL,
                Side.BUY,
                OrderType.MARKET,
                RAW_QUANTITY,
                null
        );

        when(scaleNormalizerUtil.normalize(null, DEFAULT_SYMBOL)).thenReturn(null);
        when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

        // Act
        orderIngressService.processOrder(request, DEFAULT_USER_ID);

        // Assert
        verify(scaleNormalizerUtil).normalize(null, DEFAULT_SYMBOL);
        verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
        verifyNoMoreInteractions(scaleNormalizerUtil);
    }

    @ParameterizedTest(name = "Side: {0}")
    @EnumSource(Side.class)
    @DisplayName("Should process and normalize orders consistently regardless of side (BUY or SELL)")
    public void processOrder_supportsAllSides(Side side) {
        // Arrange
        OrderRequest request = new OrderRequest(
                DEFAULT_SYMBOL,
                side,
                OrderType.LIMIT,
                RAW_QUANTITY,
                RAW_PRICE
        );

        when(scaleNormalizerUtil.normalize(RAW_PRICE, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_PRICE);
        when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

        // Act
        orderIngressService.processOrder(request, DEFAULT_USER_ID);

        // Assert
        verify(scaleNormalizerUtil).normalize(RAW_PRICE, DEFAULT_SYMBOL);
        verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
    }

    @Test
    @DisplayName("Should propagate ResponseStatusException when scale normalization rejects an invalid decimal")
    public void processOrder_whenScaleNormalizerThrows_propagatesException() {
        // Arrange
        OrderRequest request = new OrderRequest(
                DEFAULT_SYMBOL,
                Side.BUY,
                OrderType.LIMIT,
                RAW_QUANTITY,
                RAW_PRICE
        );

        when(scaleNormalizerUtil.normalize(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset MSFT only accepts 2 decimal places"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderIngressService.processOrder(request, DEFAULT_USER_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
