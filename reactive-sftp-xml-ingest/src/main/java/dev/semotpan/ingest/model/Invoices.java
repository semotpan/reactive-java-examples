package dev.semotpan.ingest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
@JacksonXmlRootElement(localName = "invoices")
public class Invoices implements XmlDocument {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "invoice")
    private List<Invoice> invoices;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Invoice {

        @JacksonXmlProperty(localName = "id")
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String id;

        @JacksonXmlProperty(localName = "amount")
        private Double amount;

        @JacksonXmlProperty(localName = "currency")
        @JsonDeserialize(using = TrimStringDeserializer.class)
        private String currency;
    }
}
