package com.example.orders.swing;

import com.example.orders.controller.DashboardController;
import com.example.orders.controller.dto.Dtos.DashboardDto;
import swingtree.UI;
import swingtree.UIForPanel;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;

/** Übersicht mit Kennzahlen, entspricht der DashboardView der Vaadin-App. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final DashboardController controller;
    private final JLabel revenue = value();
    private final JLabel placed = value();
    private final JLabel drafts = value();
    private final JLabel shipped = value();
    private final JLabel customers = value();
    private final JLabel products = value();

    public DashboardPanel(DashboardController controller) {
        this.controller = controller;
        UI.of(this)
                .withLayout("insets 16, wrap 3, gap 12", "[grow, fill][grow, fill][grow, fill]", "[]12[][]")
                .add("span 3", UI.label("Übersicht").withFontSize(22))
                .add(card("Umsatz", revenue))
                .add(card("Offene Bestellungen", placed))
                .add(card("Entwürfe", drafts))
                .add(card("Versendet", shipped))
                .add(card("Kunden", customers))
                .add(card("Produkte", products));
        refresh();
    }

    @Override
    public void refresh() {
        DashboardDto d = controller.overview();
        revenue.setText(UiSupport.money(d.revenue(), d.currency()));
        placed.setText(String.valueOf(d.ordersByStatus().getOrDefault("PLACED", 0L)));
        drafts.setText(String.valueOf(d.ordersByStatus().getOrDefault("DRAFT", 0L)));
        shipped.setText(String.valueOf(d.ordersByStatus().getOrDefault("SHIPPED", 0L)));
        customers.setText(String.valueOf(d.customers()));
        products.setText(String.valueOf(d.products()));
    }

    String revenueText() {
        return revenue.getText();
    }

    String customersText() {
        return customers.getText();
    }

    private static JLabel value() {
        JLabel l = new JLabel("-");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 26f));
        return l;
    }

    private static UIForPanel<JPanel> card(String title, JLabel value) {
        return UI.panel("insets 12, wrap 1")
                .withBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
                .add(UI.label(title))
                .add(UI.of(value));
    }
}
