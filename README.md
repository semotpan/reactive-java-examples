# reactive-java-examples

A collection of **reactive programming examples in Java 21**, focused on **real-world, production-style use cases** built with Project Reactor and related frameworks.

---

## Purpose

This repository exists to **show how to apply reactive programming in practice** — not just theoretical operators.  
Each module is a **minimal, focused example** of a real reactive workflow such as file ingestion, streaming, messaging, or database I/O.

---

## Technologies

- **[Project Reactor](https://projectreactor.io/)** — Core reactive foundation powering Spring WebFlux.
- **[Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)** — Reactive web stack built on Reactor.
- **[Spring Integration](https://docs.spring.io/spring-integration/reference/)** — Message-driven architecture with reactive bridges and adapters (e.g. SFTP, Kafka, R2DBC).
- **[RxJava 3](https://github.com/ReactiveX/RxJava)** — Functional reactive programming library for composing asynchronous streams.
- **[gRPC Java](https://grpc.io/docs/languages/java/)** — Reactive-style service communication with bidirectional streaming.
- **[RSocket](https://rsocket.io/)** — Reactive transport protocol with backpressure and multiplexed streams.
- **[Reactor Kafka](https://projectreactor.io/docs/kafka/release/reference/)** — Reactive Kafka producer and consumer integration.
- **[Reactor RabbitMQ](https://projectreactor.io/docs/rabbitmq/release/reference/)** — Reactive messaging with RabbitMQ.
- **[R2DBC](https://r2dbc.io/)** — Reactive relational database connectivity for PostgreSQL, MySQL, etc.
- **[MongoDB Reactive Streams](https://mongodb.github.io/mongo-java-driver-reactivestreams/)** — Reactive driver for MongoDB.
- **Standard Java Reactive APIs** — [`Flow`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Flow.html), [`CompletableFuture`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html), `Publisher`, and `Subscriber`.



---

## Structure

Each folder is an **independent Maven module** with its own `README.md` and `pom.xml`.

| Example | Description |
|----------|--------------|
| [`reactive-sftp-xml-ingest`](./reactive-sftp-xml-ingest) | Streams XML files directly from SFTP, parses dynamically, processes reactively. |

---
License

MIT © 2025 Serghei Motpan
