package com.example.orders.app.config;

import com.example.orders.persistence.files.DemoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Legt beim ersten Start 5 Firmen mit je 4 .properties-Dateien (je ca. 1000 Schlüssel) an. */
@Component
@ConditionalOnProperty(name = "app.properties.demo-data", havingValue = "true")
class PropertiesDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PropertiesDemoRunner.class);

    private final Path root;

    PropertiesDemoRunner(@Value("${app.properties.root:./data/properties}") Path root) {
        this.root = root;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (DemoProperties.generateIfEmpty(root)) {
            log.info("Demo-Konfigurationen angelegt in {}", root.toAbsolutePath().normalize());
        }
    }
}
