package com.example.orders;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Liegt im Wurzelpaket, damit Spring die JPA-Entities aus backend-persistence-jpa
 * (com.example.orders.persistence.jpa.entity) automatisch findet.
 */
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
public class OrdersApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
