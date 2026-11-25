package com.example.orders.jsf;

import com.example.orders.Backend;
import com.example.orders.DemoData;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Einzige Klasse, die Quarkus (CDI) und das framework-freie Backend zusammenbringt,
 * analog zu BackendConfig in app-vaadin. Das Backend ist hier von Hand verdrahtet
 * (InMemoryBackend); JPA wäre nur ein anderer Satz Repositories + UnitOfWork.
 */
@ApplicationScoped
public class BackendProducer {

    @ConfigProperty(name = "app.demo-data", defaultValue = "true")
    boolean demoData;

    @Produces
    @Singleton // kein Client-Proxy: Backend ist final und hat öffentliche Felder
    Backend backend() {
        Backend backend = InMemoryBackend.create();
        if (demoData) {
            DemoData.seed(backend);
        }
        return backend;
    }
}
