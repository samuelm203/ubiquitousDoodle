package com.example.orders.app.config;

import com.example.orders.Backend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Legt beim ersten Start Beispieldaten an. Die Daten selbst kommen aus dem framework-freien backend-core. */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
class DemoDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);

    private final Backend backend;

    DemoDataRunner(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (com.example.orders.DemoData.seed(backend)) {
            log.info("Demo-Daten angelegt");
        }
    }
}
