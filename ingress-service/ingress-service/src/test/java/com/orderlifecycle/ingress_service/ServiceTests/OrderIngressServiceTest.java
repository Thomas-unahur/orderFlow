package com.orderlifecycle.ingress_service.ServiceTests;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.orderlifecycle.ingress_service.dto.OrderEvent;
import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.dto.enums.OrderStatus;
import com.orderlifecycle.ingress_service.dto.enums.OrderType;
import com.orderlifecycle.ingress_service.dto.enums.Side;
import com.orderlifecycle.ingress_service.publisher.OrderPublisher;
import com.orderlifecycle.ingress_service.service.OrderIngressService;
import com.orderlifecycle.ingress_service.utils.ScaleNormalizerUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        @Mock
        private OrderPublisher orderPublisher;

        @InjectMocks
        private OrderIngressService orderIngressService;

        @Test
        @DisplayName("Should normalize both price and quantity, build OrderEvent and publish to Kafka for valid LIMIT orders")
        public void processOrder_validLimitOrder_normalizesAndPublishesOrderEvent() {
                // Arrange
                OrderRequest request = new OrderRequest(
                                DEFAULT_SYMBOL,
                                Side.BUY,
                                OrderType.LIMIT,
                                RAW_QUANTITY,
                                RAW_PRICE);

                when(scaleNormalizerUtil.normalize(RAW_PRICE, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_PRICE);
                when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

                // Act
                orderIngressService.processOrder(request, DEFAULT_USER_ID);

                // Assert
                verify(scaleNormalizerUtil).normalize(RAW_PRICE, DEFAULT_SYMBOL);
                verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
                verifyNoMoreInteractions(scaleNormalizerUtil);

                ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class); // ?
                verify(orderPublisher).publishOrder(eventCaptor.capture());
                verifyNoMoreInteractions(orderPublisher);

                OrderEvent publishedEvent = eventCaptor.getValue();
                assertNotNull(publishedEvent.orderId(), "Order ID must be generated");
                assertEquals(DEFAULT_USER_ID, publishedEvent.userId(), "User ID must match caller");
                assertEquals(DEFAULT_SYMBOL, publishedEvent.symbol(), "Symbol must match request");
                assertEquals(Side.BUY, publishedEvent.side(), "Side must match request");
                assertEquals(OrderType.LIMIT, publishedEvent.type(), "Type must match request");
                assertEquals(NORMALIZED_QUANTITY, publishedEvent.quantity(), "Quantity must be normalized");
                assertEquals(NORMALIZED_PRICE, publishedEvent.price(), "Price must be normalized");
                assertEquals(OrderStatus.PENDING, publishedEvent.status(), "Initial status must be PENDING");
                assertTrue(publishedEvent.timestamp() > 0, "Timestamp in nanoseconds must be positive");
        }

        @Test
        @DisplayName("Should normalize quantity, keep price as null and publish OrderEvent for MARKET orders without a defined price")
        public void processOrder_marketOrderWithNullPrice_normalizesNullPriceAndPublishesEvent() {
                // Arrange
                OrderRequest request = new OrderRequest(
                                DEFAULT_SYMBOL,
                                Side.BUY,
                                OrderType.MARKET,
                                RAW_QUANTITY,
                                null);

                when(scaleNormalizerUtil.normalize(null, DEFAULT_SYMBOL)).thenReturn(null);
                when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

                // Act
                orderIngressService.processOrder(request, DEFAULT_USER_ID);

                // Assert
                verify(scaleNormalizerUtil).normalize(null, DEFAULT_SYMBOL);
                verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
                verifyNoMoreInteractions(scaleNormalizerUtil);

                ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
                verify(orderPublisher).publishOrder(eventCaptor.capture());
                verifyNoMoreInteractions(orderPublisher);

                OrderEvent publishedEvent = eventCaptor.getValue();
                assertNull(publishedEvent.price(), "Price must be null for MARKET order");
                assertEquals(NORMALIZED_QUANTITY, publishedEvent.quantity(), "Quantity must be normalized");
                assertEquals(OrderType.MARKET, publishedEvent.type(), "Type must be MARKET");
        }

        @ParameterizedTest(name = "Side: {0}")
        @EnumSource(Side.class)
        @DisplayName("Should process and publish orders consistently regardless of side (BUY or SELL)")
        public void processOrder_supportsAllSides(Side side) {
                // Arrange
                OrderRequest request = new OrderRequest(
                                DEFAULT_SYMBOL,
                                side,
                                OrderType.LIMIT,
                                RAW_QUANTITY,
                                RAW_PRICE);

                when(scaleNormalizerUtil.normalize(RAW_PRICE, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_PRICE);
                when(scaleNormalizerUtil.normalize(RAW_QUANTITY, DEFAULT_SYMBOL)).thenReturn(NORMALIZED_QUANTITY);

                // Act
                orderIngressService.processOrder(request, DEFAULT_USER_ID);

                // Assert
                verify(scaleNormalizerUtil).normalize(RAW_PRICE, DEFAULT_SYMBOL);
                verify(scaleNormalizerUtil).normalize(RAW_QUANTITY, DEFAULT_SYMBOL);
                verify(orderPublisher).publishOrder(any(OrderEvent.class));
        }

        @Test
        @DisplayName("Should propagate ResponseStatusException and never publish order when scale normalization fails")
        public void processOrder_whenScaleNormalizerThrows_propagatesExceptionAndNeverPublishes() {
                // Arrange
                OrderRequest request = new OrderRequest(
                                DEFAULT_SYMBOL,
                                Side.BUY,
                                OrderType.LIMIT,
                                RAW_QUANTITY,
                                RAW_PRICE);

                when(scaleNormalizerUtil.normalize(any(), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Asset MSFT only accepts 2 decimal places"));

                // Act & Assert
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> orderIngressService.processOrder(request, DEFAULT_USER_ID));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                verifyNoInteractions(orderPublisher);
        }
}
