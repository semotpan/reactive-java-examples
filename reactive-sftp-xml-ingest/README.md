# Reactive SFTP XML Ingest

_**Spring Integration + Project Reactor + Dynamic XML Parsing**_

> A small, example that polls an SFTP server, streams XML files, parses them dynamically into
> polymorphic models (Invoices, Transactions), and handles them reactively — no temp files, no blocking threads, fully
> traced and idempotent.

---

### Features

- **Streaming SFTP ingestion** using `spring-integration-sftp` (`InputStream` payloads, no file staging)
- **Reactive bridge** between Spring Integration and Project Reactor via `FluxMessageChannel`
- **Dynamic XML parsing** with Jackson `XmlMapper`
- **Backpressure control** using Reactor operators
- **Idempotent processing** with Redis `ConcurrentMetadataStore`
- **Tracing and observability** via Micrometer + Zipkin


---

### Components Overview
| Component                         | Responsibility                                                                                 |
| --------------------------------- | ---------------------------------------------------------------------------------------------- |
| IntegrationConfig                 | Defines SFTP connection, file filters, Redis-backed idempotency, and reactive message channel. |
| XmlReactiveSftpMessageHandler     | Streams and parses XML reactively with `Mono.using`, handles tracing and error tagging.        |
| XmlDocumentDeserializer           | Routes XML documents dynamically based on root element (`<invoices>`, `<transactions>`).       |
| DocumentHandler                   | Processes parsed documents (e.g., log, persist, or publish downstream).                        |

---

### Prerequisites

- Java 21
- Maven 3.9+
- Docker + Docker Compose

---

### Run tests

```shell
  cd reactive-sftp-xml-ingest
```

_Ensure docker is running_

```shell
  mvn clean compile test
```

---

### Run Locally

1. Prepare directories:

```bash
  mkdir -p ./local/sftp/upload
  mkdir -p ./local/redis
```

2. Start infrastructure

```shell
  docker compose up
```

**Services:**

| Service | Port | Description                                                  |
| ------- | ---- | ------------------------------------------------------------ |
| SFTP    | 2222 | user: `foo`, password: `secret`                              |
| Redis   | 6379 | metadata store                                               |
| Zipkin  | 9411 | tracing UI at [http://localhost:9411](http://localhost:9411) |


3. Build and run the app

```shell
  mvn clean package
  java -jar target/reactive-sftp-xml-ingest-0.0.1-SNAPSHOT.jar
```
or
```shell
  mvn spring-boot:run
```

4. Test ingestion (check files: `invoices.xml` and `transactions.xml`)

Place XML files into `./local/sftp/upload` — the poller will detect, stream, and process them once.

---

### Error Handling & Tracing

- Each message creates a Micrometer span named `sftp.message.handle`.

- Access: http://localhost:9411/zipkin/?serviceName=webflux-sftp-xml-ingester&lookback=15m&endTs=1762080463269&limit=10

| Tag              | Description      |
| ---------------- | ---------------- |
| `remote.file`    | remote file name |
| `statement.type` | parsed XML type  |
| `error.type`     | exception type   |
| `error.msg`      | error message    |


Failures produce a ParsedDocument with error details for downstream handling (e.g., DLQ or alerting).

---

### Extending the System

1. Create a new XmlDocument implementation (e.g. Payments.java)
2. Add a routing rule in XmlDocumentDeserializer:

```java
    new Rule("payments", null, Payments .class);
```

3. Adjust DocumentHandler to process the new type

Optional: match by namespace instead of tag by enhancing the Rule record.

---

### Key Code Patterns

**Safe Reactive File Processing**

```java
return Mono.using(
    () -> inputStream,
    is -> Mono.fromCallable(() -> {
            XmlDocument doc = xmlMapper.readValue(is, XmlDocument.class);
            span.tag("statement.type", doc.getClass().getSimpleName());
            return ParsedDocument.builder()
                    .filename(remoteName)
                    .payload(doc)
                    .build();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(PARSE_TIMEOUT)
        .flatMap(documentHandler::handle)
        .doOnSuccess(v -> span.tag("result", "parsed")),
    this::safeClose
).then();
```

**Dynamic XML Routing (it can be externalized)**

```java
private static final List<Rule> RULES = List.of(
        new Rule("invoices", null, Invoices.class),
        new Rule("transactions", null, Transactions.class)
);

if (root.has(rule.root) && (rule.namespace == null || root.has(rule.namespace))) {
        return codec.treeToValue(root, rule.type);
}
```

---

## Tech stack:

- Spring Boot 3.x · Spring Integration · Project Reactor · Jackson XML · Redis · Micrometer Tracing


