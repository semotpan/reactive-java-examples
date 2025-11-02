package dev.semotpan;

import dev.semotpan.model.Invoices;
import dev.semotpan.model.Transactions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Slf4j
@Testcontainers
@ExtendWith(SpringExtension.class)
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = "classpath:/application-test.properties")
class E2eTest {

    private static final String SFTP_IMAGE = "atmoz/sftp:alpine";
    private static final String USER = "test";
    private static final String PASS = "pass";
    private static final String HOME = "/home/" + USER;
    private static final String UPLOAD_DIR = "upload";
    private static final String REMOTE_UPLOAD = HOME + "/" + UPLOAD_DIR + "/";
    private static final long VERIFY_TIMEOUT_MS = 10_000;

    // atmoz/sftp user syntax: "user:pass:uid:gid:dir"
    @Container
    static final GenericContainer<?> sftp =
            new GenericContainer<>(DockerImageName.parse(SFTP_IMAGE))
                    .withExposedPorts(22)
                    .withCommand("%s:%s:1001:1001:%s".formatted(USER, PASS, UPLOAD_DIR))
                    .waitingFor(Wait.forListeningPort())
                    .withLogConsumer(new Slf4jLogConsumer(log));

    @MockitoSpyBean
    DocumentHandler documentHandler;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.sftp.host", sftp::getHost);
        registry.add("app.sftp.port", () -> sftp.getMappedPort(22));
        registry.add("app.sftp.user", () -> USER);
        registry.add("app.sftp.password", () -> PASS);
        registry.add("app.sftp.remote-directory", () -> UPLOAD_DIR);
        registry.add("app.sftp.poll-interval-ms", () -> 300);
    }

    @BeforeEach
    void resetSpies() {
        Mockito.reset(documentHandler);
    }

    @Test
    void ingestsInvoicesXmlFromSftpAndInvokesHandler() {
        // Arrange
        final String filename = "invoices-e2e.xml";
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <invoices>
                  <invoice>
                    <id>INV-5</id>
                    <amount>999.00</amount>
                    <currency>CHF</currency>
                  </invoice>
                </invoices>
                """;

        final Invoices expected = Invoices.builder()
                .invoices(List.of(Invoices.Invoice.builder()
                        .id("INV-5")
                        .amount(999D)
                        .currency("CHF")
                        .build()))
                .build();

        uploadXml(filename, xml);

        // Act + Assert
        verify(documentHandler, timeout(VERIFY_TIMEOUT_MS)).ingest(argThat(pd ->
                expected.equals(pd.document()) && filename.equals(pd.filename())
        ));
        verifyNoMoreInteractions(documentHandler);
    }

    @Test
    void ingestsTransactionsXmlFromSftpAndInvokesHandler() {
        // Arrange
        final String filename = "transactions-e2e.xml";
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transactions>
                    <transaction>
                        <id>TX-2001</id>
                        <postingDate>2025-10-31</postingDate>
                        <amount Ccy="EUR">1450.00</amount>
                        <direction>CRDT</direction>
                        <reference>INV-1001</reference>
                        <counterparty>Acme Ltd</counterparty>
                    </transaction>
                    <transaction>
                        <id>TX-2002</id>
                        <postingDate>2025-11-01</postingDate>
                        <amount Ccy="USD">-75.50</amount>
                        <direction>DBIT</direction>
                        <reference>SUBS-STREAM</reference>
                        <counterparty>StreamCo</counterparty>
                    </transaction>
                </transactions>
                """;

        final Transactions expected = Transactions.builder()
                .transactions(List.of(
                        Transactions.Transaction.builder()
                                .id("TX-2001")
                                .postingDate(LocalDate.parse("2025-10-31"))
                                .amount(new Transactions.Money("EUR", new BigDecimal("1450.00")))
                                .direction(Transactions.Direction.CRDT)
                                .reference("INV-1001")
                                .counterparty("Acme Ltd")
                                .build(),
                        Transactions.Transaction.builder()
                                .id("TX-2002")
                                .postingDate(LocalDate.parse("2025-11-01"))
                                .amount(new Transactions.Money("USD", new BigDecimal("-75.50")))
                                .direction(Transactions.Direction.DBIT)
                                .reference("SUBS-STREAM")
                                .counterparty("StreamCo")
                                .build()
                ))
                .build();

        uploadXml(filename, xml);

        // Act + Assert
        verify(documentHandler, timeout(VERIFY_TIMEOUT_MS)).ingest(argThat(pd ->
                expected.equals(pd.document()) && filename.equals(pd.filename())
        ));
    }

    @Test
    void ingestsUnresolvedXmlFromSftpAndInvokesHandler() {
        // Arrange
        final String filename = "orders-e2e.xml";
        final String xml = """
                <orders>
                  <order>
                    <id>ORD-1</id>
                    <title>999.00</title>
                  </order>
                </orders>
                """;

        final String errorPrefix = "Unsupported XML for XmlDocument: cannot resolve subtype via namespace";

        uploadXml(filename, xml);

        // Act + Assert
        verify(documentHandler, timeout(VERIFY_TIMEOUT_MS)).ingest(argThat(pd ->
                pd.document() == null && filename.equals(pd.filename()) && pd.error() != null && pd.error().startsWith(errorPrefix)
        ));
    }

    private static void uploadXml(String filename, String xml) {
        sftp.copyFileToContainer(
                Transferable.of(xml.getBytes(StandardCharsets.UTF_8)),
                REMOTE_UPLOAD + filename
        );
        log.info("Uploaded {} to SFTP {}", filename, REMOTE_UPLOAD);
    }

    @AfterAll
    static void tearDown() {
        if (sftp != null) {
            sftp.close();
        }
    }
}
