package com.example.orders.javafx;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.DashboardDto;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

class DashboardTab extends Tab implements Refreshable {

    private final Backend backend;
    private final FlowPane cards = new FlowPane(16, 16);

    DashboardTab(Backend backend) {
        super("Dashboard");
        this.backend = backend;
        setClosable(false);
        cards.setPadding(new Insets(16));
        Label framework = new Label("Framework: JavaFX");
        framework.setStyle("-fx-text-fill: #666;");
        framework.setPadding(new Insets(16, 16, 0, 16));
        setContent(new VBox(framework, cards));
    }

    @Override
    public void refresh() {
        DashboardDto d = backend.dashboard.overview();
        cards.getChildren().setAll(
                card("Umsatz", Ui.money(d.revenue(), d.currency())),
                card("Offene Bestellungen", String.valueOf(d.ordersByStatus().getOrDefault("PLACED", 0L))),
                card("Entwürfe", String.valueOf(d.ordersByStatus().getOrDefault("DRAFT", 0L))),
                card("Versendet", String.valueOf(d.ordersByStatus().getOrDefault("SHIPPED", 0L))),
                card("Kunden", String.valueOf(d.customers())),
                card("Produkte", String.valueOf(d.products())));
    }

    private static VBox card(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #666;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        VBox box = new VBox(4, l, v);
        box.setPadding(new Insets(14, 22, 14, 22));
        box.setMinWidth(200);
        box.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10;");
        return box;
    }
}
