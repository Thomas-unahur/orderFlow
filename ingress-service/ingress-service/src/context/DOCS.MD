# Order Lifecycle: Ingress Service

## General Description
The Ingress Service is a microservice designed to act as the primary entry point for all incoming trading orders within the order lifecycle system. Its main purpose is to receive, validate, and securely ingest order requests from clients before they are processed by downstream services. It solves the problem of centralizing order reception, ensuring that only authenticated and syntactically valid orders enter the core system.

## Class Structure

### IngressServiceApplication
**Purpose:** The main entry point for the Spring Boot application.
**Responsibilities:**
- Bootstraps and launches the Spring application context.
- Configures auto-configuration for the microservice.

### OrderController
**Purpose:** A REST controller that exposes HTTP endpoints for order operations.
**Responsibilities:**
- Handles incoming `POST` requests to the `/orders` endpoint.
- Triggers validation on the incoming payload.
- Returns the initial HTTP response (e.g., HTTP 202 Accepted) acknowledging the receipt of the order.

### OrderRequest
**Purpose:** A Data Transfer Object (DTO) record that encapsulates the data of an incoming order.
**Responsibilities:**
- Holds the data for an order (symbol, side, type, quantity, price).
- Enforces data integrity through validation annotations (e.g., checking for nulls, positive values).
- Sanitizes input data (e.g., trimming and converting the symbol to uppercase).
- Performs complex validation logic, such as ensuring a price is provided and greater than zero specifically for LIMIT orders.

### OrderType
**Purpose:** An enumeration representing the available execution types for an order.
**Responsibilities:**
- Defines the constants `LIMIT` and `MARKET` to standardize order types across the service.

### Side
**Purpose:** An enumeration representing the direction of the trade.
**Responsibilities:**
- Defines the constants `BUY` and `SELL` to categorize order directions.

### JwtAuthFilter
**Purpose:** A security filter that runs once per request to handle JWT-based authentication.
**Responsibilities:**
- Intercepts incoming HTTP requests to check for the `Authorization` header.
- Extracts the JWT token from the `Bearer` string.
- Delegates token validation to the `JwtProvider`.
- Sets the user authentication details in the `SecurityContextHolder` if the token is valid.

### JwtProvider
**Purpose:** A utility component responsible for all cryptographic operations related to JSON Web Tokens (JWT).
**Responsibilities:**
- Generates new JWTs signed with a secure HMAC SHA-256 secret key.
- Parses and verifies the signature of incoming JWTs.
- Extracts claims from tokens, specifically the user ID (subject).
- Validates the expiration date of tokens to ensure they are still active.

### SecurityConfig
**Purpose:** The central configuration class for Spring Security.
**Responsibilities:**
- Disables unnecessary default protections like CSRF and CORS for stateless API usage.
- Configures the application to use stateless session management.
- Secures specific endpoints (e.g., requiring authentication for `POST` requests to the orders API).
- Registers the `JwtAuthFilter` in the security filter chain to process authentication before standard username/password filters.

### OrderEvent
**Purpose:** A Data Transfer Object (DTO) record that encapsulates an order event to be published to a message broker.
**Responsibilities:**
- Represents an immutable event indicating an order has been successfully ingested.
- Holds the comprehensive state of the order including its unique ID, user ID, symbol, side, type, quantity, price, status, and timestamp.
- Enforces data integrity through Jakarta Validation annotations before event publication.

### ScaleNormalizerUtil
**Purpose:** A utility component designed to normalize the decimal scale of financial values.
**Responsibilities:**
- Gracefully handles `null` values by returning them as-is (e.g., for MARKET orders).
- Validates that a given `BigDecimal` value does not exceed the maximum allowed decimal scale.
- Throws an HTTP 400 Bad Request exception (`ResponseStatusException`) if the provided scale is invalid.
- Normalizes the value by applying the expected scale and a `HALF_UP` rounding mode.

### OrderStatus
**Purpose:** An enumeration representing the various states an order can transition through during its lifecycle.
**Responsibilities:**
- Defines the standard lifecycle states: `NEW`, `PENDING`, `FILLED`, `CANCELLED`, and `REJECTED`.

### OrderIngressService
**Purpose:** The core business logic service responsible for processing incoming orders.
**Responsibilities:**
- Orchestrates the initial processing of an `OrderRequest`.
- Normalizes order quantities and prices utilizing `ScaleNormalizerUtil`.
- Generates a unique tracking ID (`UUID`) and calculates a high-precision timestamp (nanoseconds).
- Constructs an immutable `OrderEvent` representing the ingested order, ready for downstream processing.
