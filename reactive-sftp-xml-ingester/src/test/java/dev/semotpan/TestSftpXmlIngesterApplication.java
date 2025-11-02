package dev.semotpan;

import org.springframework.boot.SpringApplication;

public class TestSftpXmlIngesterApplication {

    public static void main(String[] args) {
        SpringApplication.from(ReactiveSftpXmlIngesterApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
