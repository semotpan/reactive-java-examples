package dev.semotpan.sftp;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.semotpan.DocumentHandler;
import dev.semotpan.XmlParseResult;
import dev.semotpan.model.XmlDocument;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
final class XmlReactiveSftpMessageHandler implements ReactiveMessageHandler {

    private static final Duration PARSE_TIMEOUT = Duration.ofMinutes(15);

    private final XmlMapper xmlMapper;
    private final Tracer tracer;
    private final DocumentHandler documentHandler;

    @Override
    public Mono<Void> handleMessage(Message<?> message) {
        final String remoteName = (String) message.getHeaders().getOrDefault(FileHeaders.REMOTE_FILE, "<unknown>");

        final Span span = tracer.nextSpan()
                .name("sftp.message.handle")
                .tag("remote.file", remoteName)
                .start();

        return Mono.deferContextual(ctx -> doHandle(message, remoteName, span))
                .onErrorResume(ex -> {
                    tagError(span, ex);
                    log.error("SFTP ingest failed for {}: {}", remoteName, ex.getMessage(), ex);
                    return documentHandler.ingest(XmlParseResult.failure(remoteName, ex.getMessage()));
                })
                .doFinally(sig -> span.end());
    }

    private Mono<Void> doHandle(Message<?> message, String remoteName, Span span) {
        final Object payload = message.getPayload();
        if (!(payload instanceof InputStream inputStream)) {
            return Mono.error(new PayloadTypeException(remoteName, payload));
        }

        return Mono.using(
                        () -> inputStream,
                        is -> parse(remoteName, is, span)
                                .timeout(PARSE_TIMEOUT)
                                .flatMap(documentHandler::ingest)
                                .doOnSuccess(v -> span.tag("result", "parsed")),
                        this::safeClose
                )
                .then();
    }

    private Mono<XmlParseResult> parse(String remoteName, InputStream is, Span span) {
        log.debug("SFTP ingesting file {}", remoteName);

        return Mono.fromCallable(() -> {
                    XmlDocument doc = xmlMapper.readValue(is, XmlDocument.class);
                    span.tag("statement.type", doc.getClass().getSimpleName());
                    return XmlParseResult.success(remoteName, doc);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void tagError(io.micrometer.tracing.Span span, Throwable ex) {
        span.tag("error", "true");
        span.tag("error.type", ex.getClass().getSimpleName());
        span.tag("error.msg", safeMsg(ex));
    }

    private void safeClose(InputStream is) {
        try {
            is.close();
        } catch (Exception ignore) {
            // ignored
        }
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) {
            return t.getClass().getSimpleName();
        }
        if (m.length() > 500) {
            return m.substring(0, 500);
        }
        return m;
    }

    static final class PayloadTypeException extends RuntimeException {

        PayloadTypeException(String name, Object payload) {
            super("Unexpected document type for '" + name + "': " + (payload == null ? "null" : payload.getClass().getName()));
        }
    }
}
