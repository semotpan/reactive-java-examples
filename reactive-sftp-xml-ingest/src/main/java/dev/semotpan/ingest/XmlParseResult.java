package dev.semotpan.ingest;

import dev.semotpan.ingest.model.XmlDocument;
import lombok.Builder;

@Builder
public record XmlParseResult(String filename, XmlDocument document, String error) {

    public static XmlParseResult success(String filename, XmlDocument document) {
        return XmlParseResult.builder()
                .filename(filename)
                .document(document)
                .build();
    }

    public static XmlParseResult failure(String filename, String error) {
        return XmlParseResult.builder()
                .filename(filename)
                .error(error)
                .build();
    }
}
