# Order Lifecycle: Engine Service

## General Description
The Engine Service is an independent microservice responsible for consuming, processing, and executing trading order events within the order lifecycle system. Acting as a core downstream consumer in an event-driven architecture, it ingests validated orders dispatched by the `ingress-service` through Apache Kafka and prepares them for business execution, matching, and lifecycle state management.

### Architecture and Contract Replication
To preserve decoupling and service autonomy without introducing a shared library dependency at this development stage, the domain Data Transfer Objects and enumerations (`OrderEvent`, `OrderType`, `OrderStatus`, and `Side`) are copied directly from `ingress-service` into `engine`. 

Inter-service serialization compatibility across differing Java package structures (`com.orderlifecycle.ingress_service.dto` vs. `com.core.engine.dto`) is maintained by configuring `spring.json.use.type.headers: false` in `application.yaml`, binding JSON payloads directly to `com.core.engine.dto.OrderEvent`.

## Class Structure

### EngineApplication
**Purpose:** The main entry point for the Spring Boot application.
**Responsibilities:**
- Bootstraps and launches the Spring application context.
- Configures auto-configuration for the microservice.

### OrderConsumer
**Purpose:** A message listener component responsible for ingesting, tracing, and acknowledging order events from the Kafka message broker.
**Responsibilities:**
- Listens to the Kafka `orders` topic configured via `${app.kafka.topics.orders}` under the consumer group `${spring.kafka.consumer.group-id}`.
- Deserializes Kafka records into immutable `OrderEvent` DTO instances.
- Extracts and logs partition metadata, record offsets, message keys (symbol), and order IDs for tracing.
- Delegates incoming events to internal order processing logic (`processOrder`).
- Manages manual offset acknowledgment (`Acknowledgment.acknowledge()`) under `AckMode.MANUAL_IMMEDIATE` to ensure at-least-once delivery semantics.
- Retains partition offsets and propagates runtime exceptions when processing fails, preventing premature commit of unhandled records.

### OrderEvent
**Purpose:** A Data Transfer Object (DTO) record that encapsulates an immutable order event consumed from the message broker.
**Responsibilities:**
- Represents the immutable state of an ingested order received from Kafka.
- Replicates the schema of `ingress-service`'s `OrderEvent` to preserve contract compatibility while maintaining decoupled service implementations.
- Holds order attributes including `orderId` (UUID), `userId`, `symbol`, `side`, `type`, `quantity`, `price`, `status`, and `timestamp`.
- Enforces data integrity using Jakarta Validation annotations (`@NotNull`, `@NotBlank`, `@Positive`).

### OrderType
**Purpose:** An enumeration representing the available execution types for an order.
**Responsibilities:**
- Defines constants `LIMIT` and `MARKET` to standardize execution modes across service boundaries (copied from `ingress-service` for simplicity).

### OrderStatus
**Purpose:** An enumeration representing the various states an order transitions through during its lifecycle.
**Responsibilities:**
- Defines lifecycle states: `NEW`, `PENDING`, `FILLED`, `CANCELLED`, and `REJECTED` (copied from `ingress-service` for simplicity).

### Side
**Purpose:** An enumeration representing the direction of the trade.
**Responsibilities:**
- Defines constants `BUY` and `SELL` to categorize order trade directions (copied from `ingress-service` for simplicity).

### application.yaml
**Purpose:** Application configuration descriptor defining runtime settings, server networking, and Kafka consumer mechanics.
**Responsibilities:**
- Configures the HTTP server to run on port `8081` with context path `/api/v1` to prevent port collisions with `ingress-service`.
- Configures the Kafka consumer properties: consumer group ID (`engine-service-group`), offset reset strategy (`earliest`), and manual commit enforcement (`enable-auto-commit: false`).
- Configures deserialization using `StringDeserializer` for record keys and `ErrorHandlingDeserializer` wrapping `JsonDeserializer` for record values.
- Disables Spring Kafka type headers (`spring.json.use.type.headers: false`) to decouple payload deserialization from the publisher's package namespace.
- Sets listener acknowledgment mode to `manual_immediate` for explicit offset commit control.

