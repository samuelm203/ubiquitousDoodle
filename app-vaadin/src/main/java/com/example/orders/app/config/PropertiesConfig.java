package com.example.orders.app.config;

import com.example.orders.PropertiesModule;
import com.example.orders.controller.PropertiesController;
import com.example.orders.persistence.files.FilePropertiesRepository;
import com.example.orders.repository.PropertiesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/** Verdrahtet die Firmenkonfiguration (Ordner mit .properties-Dateien) mit dem framework-freien Kern. */
@Configuration(proxyBeanMethods = false)
public class PropertiesConfig {

    @Bean
    PropertiesRepository propertiesRepository(@Value("${app.properties.root:./data/properties}") Path root) {
        return new FilePropertiesRepository(root);
    }

    @Bean
    PropertiesModule propertiesModule(PropertiesRepository repository) {
        return new PropertiesModule(repository);
    }

    @Bean
    PropertiesController propertiesController(PropertiesModule module) {
        return module.controller;
    }
}
