package dev.semotpan.sftp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.semotpan.DocumentHandler;
import dev.semotpan.model.XmlDocument;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import java.io.InputStream;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(SftpProperties.class)
@RequiredArgsConstructor
class IntegrationConfig {

    private static final String INVOICE_SFTP_METADATA_STORE = "xmlSftpMetadataStore";

    private final SftpProperties sftpProperties;

    @Bean
    SessionFactory<DirEntry> sftpSessionFactory() {
        var key = sftpProperties.privateKey();
        var passphrase = sftpProperties.privateKeyPassphrase();

        log.info(
                "Creating SFTP session factory -> privateKey provided: {}, passphrase provided: {}",
                StringUtils.hasText(key),
                StringUtils.hasText(passphrase)
        );

        var factory = new DefaultSftpSessionFactory(true);
        factory.setHost(sftpProperties.host());
        factory.setPort(sftpProperties.port());
        factory.setUser(sftpProperties.user());
        factory.setPassword(sftpProperties.password());
        factory.setAllowUnknownKeys(true);
        if (StringUtils.hasText(key)) {
            factory.setPrivateKey(new FileSystemResource(key));
        }
        if (StringUtils.hasText(passphrase)) {
            factory.setPrivateKeyPassphrase(passphrase);
        }

        return new CachingSessionFactory<>(factory);
    }

    @Bean
    SftpRemoteFileTemplate sftpRemoteFileTemplate() {
        return new SftpRemoteFileTemplate(sftpSessionFactory());
    }

    @Bean
    ConcurrentMetadataStore metadataStore(RedisConnectionFactory redisConnectionFactory) {
        return new RedisMetadataStore(redisConnectionFactory, INVOICE_SFTP_METADATA_STORE);
    }

    @Bean
    XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        SimpleModule mod = new SimpleModule("XmlDocumentPoly");
        mod.addDeserializer(XmlDocument.class, new XmlDocumentDeserializer());
        xmlMapper.registerModule(mod);
        return xmlMapper;
    }

    @Bean
    @InboundChannelAdapter(channel = "stream", poller = @Poller(fixedDelay = "${app.sftp.poll-interval-ms:1000}"))
    MessageSource<InputStream> ftpMessageSource(ConcurrentMetadataStore metadataStore) {
        var source = new org.springframework.integration.sftp.inbound.SftpStreamingMessageSource(sftpRemoteFileTemplate());
        source.setRemoteDirectory(sftpProperties.remoteDirectory());
        source.setMaxFetchSize(sftpProperties.maxFetchSize());
        var filters = new CompositeFileListFilter<DirEntry>();

        filters.addFilter(new SftpSimplePatternFileListFilter(sftpProperties.filenamePattern()));
        filters.addFilter(new SftpPersistentAcceptOnceFileListFilter(metadataStore, sftpProperties.remoteDirectory()));

        source.setFilter(filters);
        return source;
    }

    @Bean
    @ServiceActivator(inputChannel = "stream")
    ReactiveMessageHandler handle(XmlMapper xmlMapper, Tracer tracer, DocumentHandler documentHandler) {
        return new XmlReactiveSftpMessageHandler(xmlMapper, tracer, documentHandler);
    }
}
