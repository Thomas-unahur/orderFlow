package com.orderlifecycle.ingress_service.JwtTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.orderlifecycle.ingress_service.controller.OrderController;
import com.orderlifecycle.ingress_service.dto.OrderRequest;
import com.orderlifecycle.ingress_service.security.JwtAuthFilter;
import com.orderlifecycle.ingress_service.security.JwtProvider;
import com.orderlifecycle.ingress_service.security.SecurityConfig;
import com.orderlifecycle.ingress_service.service.OrderIngressService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({ SecurityConfig.class, JwtAuthFilter.class })
@WebMvcTest(OrderController.class)
@DisplayName("JwtAuthFilter Security Integration Tests")
public class JwtFilterTest {

    private static final String ORDERS_ENDPOINT = "/orders";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String TEST_USER_ID = "user-123";

    private static final String VALID_BODY = """
            {
                "symbol": "MSFT",
                "side": "BUY",
                "type": "LIMIT",
                "quantity": 10.0,
                "price": 400.0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderIngressService orderIngressService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("Should authenticate request and return 202 Accepted when token is cryptographically valid")
    public void testWithValidToken() throws Exception {
        // Arrange
        when(jwtProvider.isValidToken(VALID_TOKEN)).thenReturn(true);
        when(jwtProvider.extractUserId(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isAccepted());

        verify(orderIngressService).processOrder(any(OrderRequest.class), eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("Should return 403 Forbidden and block service access when token validation fails")
    public void testWithInvalidToken() throws Exception {
        // Arrange
        when(jwtProvider.isValidToken(INVALID_TOKEN)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + INVALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderIngressService);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when Authorization header is absent")
    public void testWithMissingAuthorizationHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderIngressService);
    }

    @ParameterizedTest(name = "Header variation: {0}")
    @ValueSource(strings = {
            "Basic dXNlcjpwYXNz",
            "Bearer",
            "Token some-value",
            "bearer lowercase-prefix"
    })
    @DisplayName("Should return 403 Forbidden when Authorization header schema is malformed or unsupported")
    public void testWithMalformedAuthorizationHeader(String malformedHeader) throws Exception {
        // Act & Assert
        mockMvc.perform(post(ORDERS_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, malformedHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderIngressService);
    }
}
