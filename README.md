<h1>OrderFlow: Arquitectura de Microservicios y Motor de Emparejamiento Bursátil</h1>

<p>Sistema backend de alta concurrencia diseñado para modelar el Ciclo de Vida Transaccional de una Orden en un entorno financiero, abarcando desde la ingesta de peticiones REST hasta la ejecución en memoria y la liquidación definitiva bajo propiedades ACID.</p>

<h2>1. Arquitectura del Sistema (Diagrama Lógico)</h2>

<p>La plataforma está diseñada mediante una arquitectura de microservicios desacoplada, separando las responsabilidades públicas de entrada, la infraestructura de mensajería asíncrona y el motor transaccional privado.</p>

<img width="2816" height="1536" alt="Diagramada-de-arquitectura-de-microservicios" src="https://github.com/user-attachments/assets/b2fb399a-7c9b-4133-8aa6-a2c4644a61eb" />


<h3>Componentes Clave</h3>

<p><strong>Caja 1: API Gateway (Microservicio Público - Ingress API)</strong></p>
<ul>
  <li><strong>Stack:</strong> Java 21, Spring Boot, Spring Security, JWT.</li>
  <li><strong>Responsabilidad:</strong> Actúa como la puerta de entrada segura. Recibe la petición REST del usuario, valida el token JWT, aplica reglas estrictas de validación de formato (normalización de escalas con BigDecimal) y delega el procesamiento publicando un evento asíncrono. Es un componente stateless (sin base de datos propia).</li>
</ul>

<p><strong>Caja 2: El Puente Asíncrono (Infraestructura - Message Broker)</strong></p>
<ul>
  <li><strong>Stack:</strong> Apache Kafka / RabbitMQ.</li>
  <li><strong>Responsabilidad:</strong> Garantiza la persistencia temporal de los eventos. Desacopla la velocidad de ingesta del procesamiento pesado del núcleo, evitando la pérdida de datos ante picos de tráfico o caídas del motor.</li>
</ul>

<p><strong>Caja 3: El Cerebro (Microservicio Privado - Core Engine)</strong></p>
<ul>
  <li><strong>Stack:</strong> Java 21 con concurrencia avanzada, Spring Data JPA, MySQL Ledger.</li>
  <li><strong>Responsabilidad:</strong> Consume los eventos en segundo plano, realiza la validación y retención de riesgo (Pre-Trade) asegurando transacciones ACID, y administra el libro de órdenes (Order Book) en memoria RAM mediante estructuras de datos thread-safe.</li>
</ul>

<h2>2. Glosario del Dominio (Lenguaje Ubicuo y Nouns)</h2>

<p>Para mantener la rigorosidad técnica y contable propia del sector bursátil, se define el siguiente vocabulario y modelo de entidades:</p>

<h3>Conceptos Core</h3>

<ul>
  <li><strong>Asset / Instrumento:</strong> Activo financiero negociable (ej. acciones como MSFT, pares de Forex, criptomonedas).</li>
  <li><strong>Base Asset vs. Quote Asset:</strong> En un par comercial (ej. MSFT/USD), el Base Asset es el activo que se compra/vende (MSFT) y el Quote Asset es la moneda de cotización con la que se paga (USD).</li>
  <li><strong>Order Book (Libro de Órdenes):</strong> Estructura en memoria que agrupa las intenciones del mercado. Se divide en:
    <ul>
      <li><strong>Bids (Compras):</strong> Órdenes ordenadas de mayor a menor precio.</li>
      <li><strong>Asks (Ventas):</strong> Órdenes ordenadas de menor a mayor precio.</li>
    </ul>
  </li>
  <li><strong>Maker / Taker:</strong>
    <ul>
      <li><strong>Maker:</strong> Orden que ingresa al libro y queda reposando aportando liquidez.</li>
      <li><strong>Taker:</strong> Orden que llega y cruza de inmediato contra una orden existente, tomando esa liquidez.</li>
    </ul>
  </li>
  <li><strong>Settlement (Liquidación):</strong> Momento transaccional donde los saldos de las cuentas se actualizan de manera definitiva.</li>
</ul>

<h3>Entidades Clave (Nouns)</h3>

<p><strong>Wallet / Cuenta (Billetera):</strong> El usuario posee una billetera independiente por cada activo.</p>
<ul>
  <li><strong>Campos principales:</strong> userId, asset, availableBalance (disponible para operar), lockedBalance (fondos retenidos temporalmente por órdenes abiertas).</li>
  <li><strong>Regla técnica:</strong> Uso estricto de java.math.BigDecimal para evitar errores de coma flotante.</li>
</ul>

<p><strong>Order (Orden):</strong> La intención de operación enviada por el participante.</p>
<ul>
  <li><strong>Campos principales:</strong> orderId, accountId, symbol (ej. MSFT/USD), side (BUY/SELL), type (MARKET/LIMIT), price, quantity, status (PENDING, PARTIAL, FILLED, CANCELED), y timestamp (con precisión de nanosegundos para garantizar el estricto orden FIFO - First In, First Out).</li>
</ul>

<p><strong>Trade (Ejecución):</strong> Registro histórico e inmutable del emparejamiento exitoso entre dos partes.</p>
<ul>
  <li><strong>Campos principales:</strong> tradeId, symbol, price, quantity, makerOrderId, takerOrderId, timestamp.</li>
</ul>

<h2>3. Flujo Transaccional Paso a Paso (Happy Path)</h2>

<p>El ciclo de vida completo de una orden de compra limitada sobre acciones de Microsoft (MSFT) se compone de cuatro fases secuenciales:</p>

<img width="984" height="529" alt="Diagrama de Secuencia del Flujo Principal" src="https://github.com/user-attachments/assets/bb99fc97-9892-40ec-a6ad-39b571b8cc58" />


<h3>Detalle de Fases</h3>

<h4>Fase 1: Recepción (Ingress API)</h4>
<ul>
  <li>El Usuario envía una petición REST HTTP POST enviando una orden Limit BUY MSFT (1 acción) a precio X.</li>
  <li>La Ingress API valida las credenciales de seguridad mediante el token JWT.</li>
  <li>La Ingress API valida el formato y la precisión de los campos numéricos utilizando BigDecimal y reglas de escala estandarizada.</li>
</ul>

<h4>Fase 2: Encolamiento (Message Broker)</h4>
<ul>
  <li>La Ingress API publica de forma asíncrona el evento "Orden Creada" hacia el Message Broker.</li>
  <li>El Message Broker procesa la inserción y devuelve una confirmación (Ack).</li>
  <li>La Ingress API responde de inmediato al Usuario con un código HTTP 202 ("Orden en proceso"), liberando el hilo HTTP y garantizando baja latencia.</li>
</ul>

<h4>Fase 3: Riesgo Pre-Trade (Core Engine &amp; Ledger)</h4>
<ul>
  <li>El Core Engine consume de manera asíncrona el evento desde el broker en segundo plano.</li>
  <li>El Core Engine consulta en la base de datos MySQL Ledger el balance disponible del Quote Asset (USD).</li>
  <li>El Ledger confirma la suficiencia de fondos.</li>
  <li>El Core Engine ejecuta un bloqueo transaccional estricto (ACID), reteniendo temporalmente los fondos necesarios en la billetera del usuario (lockedBalance).</li>
</ul>

<h4>Fase 4: Matching y Settlement (Order Book &amp; Ledger)</h4>
<ul>
  <li>El Core Engine inyecta la orden validada dentro del nivel correspondiente de los Bids de MSFT en el Order Book ubicado en memoria RAM.</li>
  <li>El motor busca coincidencias activas priorizando precio y orden de llegada (FIFO).</li>
  <li>Se encuentra una contraparte compatible, generando de forma inmediata un registro inmutable de ejecución (Trade).</li>
  <li>Se dispara la liquidación definitiva (Settlement) hacia la base de datos MySQL Ledger.</li>
  <li>El Ledger actualiza de manera atómica las billeteras definitivas: se debitan los fondos retenidos del Quote Asset (-USD) y se acreditan las acciones adquiridas del Base Asset (+MSFT).</li>
</ul>
