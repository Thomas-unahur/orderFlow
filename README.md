# OrderFlow: Arquitectura de Microservicios y Motor de Emparejamiento Bursátil

Sistema backend de alta concurrencia diseñado para modelar el Ciclo de Vida Transaccional de una Orden en un entorno financiero, abarcando desde la ingesta de peticiones REST hasta la ejecución en memoria y la liquidación definitiva bajo propiedades ACID.

## 1. Arquitectura del Sistema (Diagrama Lógico)

La plataforma está diseñada mediante una arquitectura de microservicios desacoplada, separando las responsabilidades públicas de entrada, la infraestructura de mensajería asíncrona y el motor transaccional privado.

<img width="1152" height="648" alt="diagrama de arquitectura" src="https://github.com/user-attachments/assets/a6ea5c38-5581-4708-9f60-d87a277a61b1" />


### Explicación del Ecosistema de Servicios

El diseño de la plataforma se divide en tres componentes principales para garantizar alta disponibilidad, baja latencia y tolerancia a fallos:

*   **Caja 1: API Gateway (Microservicio Público - Ingress API)**
    *   **Stack:** Java 21, Spring Boot, Spring Security, JWT.
    *   **Responsabilidad:** Actúa como la puerta de entrada segura. Recibe la petición REST del usuario, valida el token JWT, aplica reglas estrictas de validación de formato (normalización de escalas con `BigDecimal`) y delega el procesamiento publicando un evento asíncrono. Es un componente *stateless* (sin base de datos propia), lo que le permite escalar horizontalmente para recibir miles de peticiones sin bloqueos.
*   **Caja 2: El Puente Asíncrono (Infraestructura - Message Broker)**
    *   **Stack:** Apache Kafka / RabbitMQ.
    *   **Responsabilidad:** Garantiza la persistencia temporal de los eventos. Desacopla la velocidad de ingesta (Caja 1) del procesamiento pesado del núcleo (Caja 3). Funciona como un amortiguador (*buffer*) que evita la pérdida de datos ante picos de tráfico extremos o caídas temporales del motor de emparejamiento.
*   **Caja 3: El Cerebro (Microservicio Privado - Core Engine)**
    *   **Stack:** Java 21 con concurrencia avanzada, Spring Data JPA, MySQL Ledger.
    *   **Responsabilidad:** Consume los eventos en segundo plano desde el broker. Realiza la validación y retención de riesgo (Pre-Trade) asegurando transacciones ACID en la base de datos. Finalmente, administra el libro de órdenes (*Order Book*) en memoria RAM mediante estructuras de datos *thread-safe* para emparejar las operaciones a velocidad extrema.

## 2. Glosario del Dominio (Lenguaje Ubicuo y Nouns)

Para mantener la rigorosidad técnica y contable propia del sector bursátil, se define el siguiente vocabulario y modelo de entidades:

### Conceptos Core
*   **Asset / Instrumento:** Activo financiero negociable (ej. acciones como MSFT, pares de Forex, criptomonedas).
*   **Base Asset vs. Quote Asset:** En un par comercial (ej. MSFT/USD), el Base Asset es el activo que se compra/vende (MSFT) y el Quote Asset es la moneda de cotización con la que se paga (USD).
*   **Order Book (Libro de Órdenes):** Estructura en memoria que agrupa las intenciones del mercado. Se divide en:
    *   **Bids (Compras):** Órdenes ordenadas de mayor a menor precio.
    *   **Asks (Ventas):** Órdenes ordenadas de menor a mayor precio.
*   **Maker / Taker:**
    *   **Maker:** Orden que ingresa al libro y queda reposando aportando liquidez.
    *   **Taker:** Orden que llega y cruza de inmediato contra una orden existente, tomando esa liquidez.
*   **Settlement (Liquidación):** Momento transaccional donde los saldos de las cuentas se actualizan de manera definitiva.

### Entidades Clave (Nouns)
*   **Wallet / Cuenta (Billetera):** El usuario posee una billetera independiente por cada activo.
    *   *Campos principales:* `userId`, `asset`, `availableBalance` (disponible para operar), `lockedBalance` (fondos retenidos temporalmente por órdenes abiertas).
    *   *Regla técnica:* Uso estricto de `java.math.BigDecimal` para evitar errores de coma flotante.
*   **Order (Orden):** La intención de operación enviada por el participante.
    *   *Campos principales:* `orderId`, `accountId`, `symbol` (ej. MSFT/USD), `side` (BUY/SELL), `type` (MARKET/LIMIT), `price`, `quantity`, `status` (PENDING, PARTIAL, FILLED, CANCELED), y `timestamp` (con precisión de nanosegundos para garantizar el estricto orden FIFO - First In, First Out).
*   **Trade (Ejecución):** Registro histórico e inmutable del emparejamiento exitoso entre dos partes.
    *   *Campos principales:* `tradeId`, `symbol`, `price`, `quantity`, `makerOrderId`, `takerOrderId`, `timestamp`.

## 3. Flujo Transaccional Paso a Paso (Happy Path)

El ciclo de vida completo de una orden de compra limitada sobre acciones de Microsoft (MSFT) se compone de cinco fases secuenciales:

<img width="5141" height="2740" alt="Diagrama de Secuencia del Flujo Principal" src="https://github.com/user-attachments/assets/862936d8-961b-42c0-9b1d-94d1c30ff0be" />


### Detalle de Fases

#### Fase 1: Recepción (Ingress API)
*   El Usuario envía una petición REST HTTP POST enviando una orden Limit BUY MSFT (1 acción) a precio X.
*   La Ingress API valida las credenciales de seguridad mediante el token JWT.
*   La Ingress API valida el formato y la precisión de los campos numéricos utilizando `BigDecimal` y reglas de escala estandarizada.

#### Fase 2: Encolamiento (Message Broker)
*   La Ingress API publica de forma asíncrona el evento "Orden Creada" hacia el Message Broker.
*   El Message Broker procesa la inserción y devuelve una confirmación (Ack).
*   La Ingress API responde de inmediato al Usuario con un código HTTP 202 ("Orden en proceso"), liberando el hilo HTTP y garantizando baja latencia.

#### Fase 3: Riesgo Pre-Trade (Core Engine & Ledger)
*   El Core Engine consume de manera asíncrona el evento desde el broker en segundo plano.
*   El Core Engine consulta en la base de datos MySQL Ledger el balance disponible del Quote Asset (USD).
*   El Ledger confirma la suficiencia de fondos.
*   El Core Engine ejecuta un bloqueo transaccional estricto (ACID), reteniendo temporalmente los fondos necesarios en la billetera del usuario (`lockedBalance`).

#### Fase 4: Matching y Settlement (Order Book & Ledger)
*   El Core Engine inyecta la orden validada dentro del nivel correspondiente de los Bids de MSFT en el Order Book ubicado en memoria RAM.
*   El motor busca coincidencias activas priorizando precio y orden de llegada (FIFO).
*   Se encuentra una contraparte compatible, generando de forma inmediata un registro inmutable de ejecución (Trade).
*   Se dispara la liquidación definitiva (Settlement) hacia la base de datos MySQL Ledger.
*   El Ledger actualiza de manera atómica las billeteras definitivas: se debitan los fondos retenidos del Quote Asset (-USD) y se acreditan las acciones adquiridas del Base Asset (+MSFT).

#### Fase 5: Post-Trade y Notificación (WebSockets)
*   Una vez realizada la liquidación, el motor central (Core Engine) publica un evento asíncrono de "Orden Ejecutada" hacia el puente de mensajería (Broker)[cite: 1].
*   La Ingress API consume ese evento de liquidación desde el broker[cite: 1].
*   Finalmente, la Ingress API envía los datos hacia el cliente utilizando WebSockets, lo cual permite notificar el estado de la ejecución y la actualización de los balances en tiempo real[cite: 1].
