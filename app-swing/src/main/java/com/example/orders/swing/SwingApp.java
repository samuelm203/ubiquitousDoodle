package com.example.orders.swing;

import com.example.orders.Backend;
import com.example.orders.DemoData;
import com.example.orders.PropertiesModule;
import com.example.orders.controller.PropertiesController;
import com.example.orders.persistence.files.DemoProperties;
import com.example.orders.persistence.files.FilePropertiesRepository;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import com.formdev.flatlaf.FlatLightLaf;
import swingtree.SwingTree;
import swingtree.threading.EventProcessor;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Einstieg der Desktop-App. Das Backend läuft im selben Prozess, ohne Spring und ohne REST:
 * Die Panels sprechen nur die Controller aus {@link Backend} an.
 */
public final class SwingApp {

    private SwingApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            // Event-Handler von Swing-Tree laufen direkt im Event-Dispatch-Thread (kein Entkoppeln)
            SwingTree.get().setEventProcessor(EventProcessor.COUPLED);

            Backend backend = InMemoryBackend.create();
            DemoData.seed(backend);
            MainFrame.create(backend, propertiesFor(System.getProperty("orders.role", "USER"))).setVisible(true);
        });
    }

    /**
     * Die App kennt keine Anmeldung: Die Rolle kommt aus {@code -Dorders.role=ADMIN} (Standard USER).
     * Nur ADMIN bekommt den Konfigurations-Tab; Dateien liegen unter {@code -Dapp.properties.root} (Standard ./data/properties).
     */
    static PropertiesController propertiesFor(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return null;
        }
        Path root = Path.of(System.getProperty("app.properties.root", "./data/properties"));
        DemoProperties.generateIfEmpty(root);
        return new PropertiesModule(new FilePropertiesRepository(root)).controller;
    }
}
