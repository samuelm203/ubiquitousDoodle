package com.example.orders.app.ui;

import com.example.orders.controller.DashboardController;
import com.example.orders.controller.dto.Dtos.DashboardDto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;

@Route("dashboard")
@RouteAlias("")
@PageTitle("Dashboard | Orders")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(DashboardController controller) {
        DashboardDto d = controller.overview();

        FlexLayout cards = new FlexLayout(
                card("Umsatz", UiSupport.money(d.revenue(), d.currency())),
                card("Offene Bestellungen", String.valueOf(d.ordersByStatus().getOrDefault("PLACED", 0L))),
                card("Entwürfe", String.valueOf(d.ordersByStatus().getOrDefault("DRAFT", 0L))),
                card("Versendet", String.valueOf(d.ordersByStatus().getOrDefault("SHIPPED", 0L))),
                card("Kunden", String.valueOf(d.customers())),
                card("Produkte", String.valueOf(d.products())));
        cards.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cards.getStyle().set("gap", "var(--lumo-space-m)");

        add(new H2("Übersicht"), cards);
    }

    private static Component card(String label, String value) {
        Span l = new Span(label);
        l.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        Span v = new Span(value);
        v.getStyle().set("font-size", "var(--lumo-font-size-xxl)").set("font-weight", "600");

        Div card = new Div(l, new Div(v));
        card.getStyle()
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("min-width", "200px");
        return card;
    }
}
