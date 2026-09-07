package com.orderlifecycle.ingress_service.ControllerTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.orderlifecycle.ingress_service.controller.OrderController;
import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.security.JwtAuthFilter;
import com.orderlifecycle.ingress_service.service.OrderIngressService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController Web Layer Tests")
public class OrderControllerTest {

    private static final String ORDERS_ENDPOINT = "/orders";
    private static final String ORDER_IN_PROGRESS_MESSAGE = "Order in progress";

    private static final String VALID_ORDER_JSON = """
            {
                "symbol": "MSFT",
                "side": "BUY",
                "type": "LIMIT",
                "quantity": 1.5,
                "price": 5000.0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderIngressService orderIngressService;

    @Test
    @DisplayName("Should return 202 Accepted and invoke ingress service when request payload is valid")
    public void receiveOrder_validRequest_returnsAccepted() throws Exception {
        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_ORDER_JSON))
                .andExpect(status().isAccepted())
                .andExpect(content().string(ORDER_IN_PROGRESS_MESSAGE));

        verify(orderIngressService).processOrder(any(OrderRequest.class), any());
    }

    @ParameterizedTest(name = "Invalid payload: {0}")
    @ValueSource(strings = {
            // Missing/empty symbol
            """
            {"symbol": "", "side": "BUY", "type": "LIMIT", "quantity": 1.5, "price": 5000.0}
            """,
            // Negative quantity
            """
            {"symbol": "MSFT", "side": "BUY", "type": "LIMIT", "quantity": -1.5, "price": 5000.0}
            """,
            // Missing side
            """
            {"symbol": "MSFT", "type": "LIMIT", "quantity": 1.5, "price": 5000.0}
            """,
            // Missing type
            """
            {"symbol": "MSFT", "side": "BUY", "quantity": 1.5, "price": 5000.0}
            """,
            // Zero price for LIMIT order
            """
            {"symbol": "MSFT", "side": "BUY", "type": "LIMIT", "quantity": 1.5, "price": 0.0}
            """,
            // Missing price for LIMIT order
            """
            {"symbol": "MSFT", "side": "BUY", "type": "LIMIT", "quantity": 1.5}
            """
    })
    @DisplayName("Should return 400 Bad Request and never invoke ingress service when payload fails validation")
    public void receiveOrder_invalidRequest_returnsBadRequest(String invalidJson) throws Exception {
        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(orderIngressService, never()).processOrder(any(), any());
    }
}
