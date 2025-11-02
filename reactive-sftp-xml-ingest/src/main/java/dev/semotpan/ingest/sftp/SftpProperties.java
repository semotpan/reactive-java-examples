package dev.semotpan.ingest.sftp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sftp")
record SftpProperties(String host,
                      int port,
                      String user,
                      String password,
                      String privateKey,
                      String privateKeyPassphrase,
                      String remoteDirectory,
                      String filenamePattern,
                      long pollIntervalMs,
                      int maxFetchSize) {

}
