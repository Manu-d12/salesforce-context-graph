package org.autorabit.salesforcecontextgraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SalesforceContextGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesforceContextGraphApplication.class, args);
    }
}
