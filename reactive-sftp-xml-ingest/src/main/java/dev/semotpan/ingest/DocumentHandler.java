package dev.semotpan.ingest;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public final class DocumentHandler {

    private final XmlMapper xmlMapper;

    public Mono<Void> ingest(XmlParseResult result) {
        log.info("Parse result document filename {}", result.filename());
        log.info("Parse result document document {}", result.document());
        log.info("Parse result document error {}", result.error());

//        return writeToFileSystem(result);
        return Mono.empty();
    }

    private Mono<Void> writeToFileSystem(XmlParseResult result) {
        return Mono.fromCallable(() -> {
                    byte[] bytes = convertToFileContent(result); // includes Jackson serialization
                    if (bytes.length == 0) {
                        log.warn("No content generated for {}. Skipping write.", result.filename());
                        return null;
                    }
                    Files.write(Paths.get(result.filename()), bytes);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private byte[] convertToFileContent(XmlParseResult result) {
        try {
            return xmlMapper.writeValueAsBytes(result.document());
        } catch (IOException e) {
            log.error("Failed to generate content for {}", result.filename(), e);
            return new byte[0];
        }
    }
}
