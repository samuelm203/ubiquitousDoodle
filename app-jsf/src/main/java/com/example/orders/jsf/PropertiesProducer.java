package com.example.orders.jsf;

import com.example.orders.PropertiesModule;
import com.example.orders.persistence.files.DemoProperties;
import com.example.orders.persistence.files.FilePropertiesRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;

/** Verdrahtet die Firmenkonfiguration (Ordner mit .properties-Dateien) mit dem framework-freien Kern. */
@ApplicationScoped
public class PropertiesProducer {

    @ConfigProperty(name = "app.properties.root", defaultValue = "./data/properties")
    String root;

    @ConfigProperty(name = "app.properties.demo-data", defaultValue = "false")
    boolean demoData;

    @Produces
    @Singleton
    PropertiesModule propertiesModule() {
        Path path = Path.of(root);
        if (demoData) {
            DemoProperties.generateIfEmpty(path);
        }
        return new PropertiesModule(new FilePropertiesRepository(path));
    }
}
