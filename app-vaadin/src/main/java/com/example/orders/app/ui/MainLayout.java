package com.example.orders.app.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/** Gemeinsamer Rahmen (Header + Navigation) für alle Views. */
@Layout
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext auth) {
        H1 title = new H1("Orders");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        Span user = new Span(auth.getPrincipalName().orElse(""));
        Button logout = new Button("Abmelden", VaadinIcon.SIGN_OUT.create(), e -> auth.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, user, logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.getStyle().set("padding-right", "var(--lumo-space-m)");
        addToNavbar(header);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Bestellungen", OrdersView.class, VaadinIcon.CART.create()));
        nav.addItem(new SideNavItem("Kunden", CustomersView.class, VaadinIcon.USERS.create()));
        if (auth.hasRole("ADMIN")) {
            nav.addItem(new SideNavItem("Produkte", ProductsView.class, VaadinIcon.PACKAGE.create()));
        }
        addToDrawer(nav);
    }
}
