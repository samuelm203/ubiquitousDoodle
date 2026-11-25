package com.example.orders.swing;

import com.example.orders.Backend;
import com.example.orders.DemoData;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.persistence.inmemory.InMemoryBackend;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import swingtree.SwingTree;
import swingtree.threading.EventProcessor;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Headless-Tests: Die Panels werden ohne sichtbares Fenster erzeugt und über ihre Bedienelemente angesteuert.
 * Geprüft wird die Anbindung an die Controller, nicht das Aussehen.
 */
class SwingPanelsTest {

    private final List<String> errors = new ArrayList<>();
    private Backend backend;

    @BeforeAll
    static void headless() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED);
    }

    @BeforeEach
    void setUp() {
        errors.clear();
        UiSupport.setDialogs(new UiSupport.Dialogs() {
            @Override
            public void error(String message) {
                errors.add(message);
            }

            @Override
            public boolean confirm(String message) {
                return true;
            }
        });
        backend = InMemoryBackend.create();
        DemoData.seed(backend);
    }

    private static void layout(JPanel p) {
        p.setSize(new Dimension(1100, 600));
        p.doLayout();
    }

    @Test
    void dashboardShowsStats() {
        DashboardPanel panel = new DashboardPanel(backend.dashboard);
        layout(panel);
        assertThat(panel.customersText()).isEqualTo(String.valueOf(backend.dashboard.overview().customers()));
        assertThat(panel.revenueText()).startsWith("CHF");
    }

    @Test
    void customerCreateUpdateDelete() {
        CustomersPanel panel = new CustomersPanel(backend.customers);
        layout(panel);
        int before = panel.model().getRowCount();

        panel.edit(new CustomerDto(null, "", "", "", true));
        panel.nameField().setText("Swing AG");
        panel.emailField().setText("info@swing.ch");
        panel.cityField().setText("Bern");
        panel.save();
        assertThat(errors).isEmpty();
        assertThat(panel.model().getRowCount()).isEqualTo(before + 1);

        CustomerDto created = panel.model().rows().stream().filter(c -> c.name().equals("Swing AG")).findFirst().orElseThrow();
        panel.edit(created);
        panel.cityField().setText("Basel");
        panel.save();
        assertThat(backend.customers.get(created.id()).city()).isEqualTo("Basel");

        panel.edit(backend.customers.get(created.id()));
        panel.delete();
        assertThat(panel.model().getRowCount()).isEqualTo(before);
    }

    @Test
    void invalidCustomerShowsDialogInsteadOfThrowing() {
        CustomersPanel panel = new CustomersPanel(backend.customers);
        panel.edit(new CustomerDto(null, "", "", "", true));
        panel.nameField().setText("X");
        panel.emailField().setText("kein-email");
        panel.save();
        assertThat(errors).hasSize(1);
    }

    @Test
    void productCreate() {
        ProductsPanel panel = new ProductsPanel(backend.products);
        layout(panel);
        int before = panel.model().getRowCount();
        panel.edit(new ProductDto(null, "", "", java.math.BigDecimal.ZERO, "CHF", true));
        panel.skuField().setText("SW-1");
        panel.nameField().setText("Swing-Produkt");
        panel.priceField().setText("12,50");
        panel.save();
        assertThat(errors).isEmpty();
        assertThat(panel.model().getRowCount()).isEqualTo(before + 1);
    }

    @Test
    void orderLifecycle() {
        OrdersPanel panel = new OrdersPanel(backend.orders, backend.customers, backend.products);
        layout(panel);
        int before = panel.orderModel().getRowCount();

        // ohne Kunde: Dialog
        panel.createOrder();
        assertThat(errors).hasSize(1);

        panel.customerCombo().setSelectedIndex(0);
        panel.createOrder();
        assertThat(panel.orderModel().getRowCount()).isEqualTo(before + 1);
        assertThat(panel.detailPanel().isVisible()).isTrue();
        assertThat(panel.placeButton().isVisible()).isTrue();

        // leere Bestellung aufgeben: fachlicher Fehler als Dialog
        errors.clear();
        panel.placeButton().doClick();
        assertThat(errors).hasSize(1);

        panel.productCombo().setSelectedIndex(0);
        panel.quantitySpinner().setValue(2);
        panel.addLineButton().doClick();
        assertThat(errors).hasSize(1);
        assertThat(panel.lineModel().getRowCount()).isEqualTo(1);

        panel.lineTable().setRowSelectionInterval(0, 0);
        panel.removeLineButton().doClick();
        assertThat(panel.lineModel().getRowCount()).isZero();

        panel.productCombo().setSelectedIndex(0);
        panel.addLineButton().doClick();
        panel.placeButton().doClick();
        OrderDto placed = ListTableModel.selected(panel.orderTable(), panel.orderModel());
        assertThat(placed.status()).isEqualTo("PLACED");
        assertThat(panel.shipButton().isVisible()).isTrue();

        panel.shipButton().doClick();
        assertThat(backend.orders.get(placed.id()).status()).isEqualTo("SHIPPED");
        assertThat(panel.cancelButton().isVisible()).isFalse();
    }

    @Test
    void mainFrameBuildsWhenDisplayAvailable() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        var frame = MainFrame.create(backend);
        assertThat(frame.getTitle()).contains("Orders");
        frame.dispose();
    }
}
