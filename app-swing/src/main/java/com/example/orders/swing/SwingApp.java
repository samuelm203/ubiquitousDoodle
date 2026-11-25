package com.example.orders.swing;

import com.example.orders.Backend;
import com.example.orders.DemoData;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import com.formdev.flatlaf.FlatLightLaf;
import swingtree.SwingTree;
import swingtree.threading.EventProcessor;

import javax.swing.SwingUtilities;

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
            MainFrame.create(backend).setVisible(true);
        });
    }
}
