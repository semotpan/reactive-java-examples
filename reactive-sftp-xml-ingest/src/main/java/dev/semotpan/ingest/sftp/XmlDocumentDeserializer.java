package dev.semotpan.ingest.sftp;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.semotpan.ingest.model.Invoices;
import dev.semotpan.ingest.model.Transactions;
import dev.semotpan.ingest.model.XmlDocument;

import java.io.IOException;
import java.util.List;

final class XmlDocumentDeserializer extends JsonDeserializer<XmlDocument> {

    private static final List<Rule> RULES = List.of(
            new Rule("invoice", null, Invoices.class),
            new Rule("transaction", null, Transactions.class)
    );

    @Override
    public XmlDocument deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode root = codec.readTree(p);

        for (Rule rule : RULES) {
            if (root.has(rule.root) && (rule.namespace == null || root.has(rule.namespace))) {
                return codec.treeToValue(root, rule.type);
            }
        }

        throw JsonMappingException.from(p, "Unsupported XML for XmlDocument: cannot resolve subtype via namespace");
    }

    record Rule(String root, String namespace, Class<? extends XmlDocument> type) {
    }
}
