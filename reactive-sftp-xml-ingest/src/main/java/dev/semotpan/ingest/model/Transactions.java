package dev.semotpan.ingest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
@JacksonXmlRootElement(localName = "transactions")
public class Transactions implements XmlDocument {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "transaction")
    private List<Transaction> transactions;

    public enum Direction {
        CRDT,
        DBIT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transaction {

        @JacksonXmlProperty(localName = "id")
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String id;

        @JacksonXmlProperty(localName = "postingDate")
        private LocalDate postingDate;

        @JacksonXmlProperty(localName = "amount")
        private Money amount;

        @JacksonXmlProperty(localName = "direction")
        private Direction direction;

        @JacksonXmlProperty(localName = "reference")
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String reference;

        @JacksonXmlProperty(localName = "counterparty")
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String counterparty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    public static class Money {

        @JacksonXmlProperty(localName = "Ccy", isAttribute = true)
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String ccy;

        @JacksonXmlText
        private BigDecimal value;
    }
}
