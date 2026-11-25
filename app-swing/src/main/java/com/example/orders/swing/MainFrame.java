package com.example.orders.swing;

import com.example.orders.Backend;
import swingtree.UI;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/** Hauptfenster: ein Tab pro Bereich. Beim Tabwechsel wird das Panel über den Controller neu geladen. */
public final class MainFrame {

    private MainFrame() {
    }

    /** Baut das Fenster (noch nicht sichtbar). Nur im Event-Dispatch-Thread aufrufen. */
    public static JFrame create(Backend backend) {
        DashboardPanel dashboard = new DashboardPanel(backend.dashboard);
        CustomersPanel customers = new CustomersPanel(backend.customers);
        ProductsPanel products = new ProductsPanel(backend.products);
        OrdersPanel orders = new OrdersPanel(backend.orders, backend.customers, backend.products);

        JTabbedPane tabs = UI.tabbedPane()
                .add(UI.tab("Dashboard").add(UI.of(dashboard)))
                .add(UI.tab("Kunden").add(UI.of(customers)))
                .add(UI.tab("Produkte").add(UI.of(products)))
                .add(UI.tab("Bestellungen").add(UI.of(orders)))
                .get(JTabbedPane.class);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() instanceof Refreshable r) {
                r.refresh();
            }
        });

        JFrame frame = new JFrame("Orders | Swing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UI.of(frame)
                .add(UI.of(tabs));
        frame.setSize(1200, 720);
        frame.setLocationRelativeTo(null);
        return frame;
    }
}
