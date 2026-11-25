package com.example.orders.persistence.inmemory;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testet Controller -> Service -> Model -> Repository, komplett ohne Framework. */
class BackendTest {

    private final Backend backend = InMemoryBackend.create(
            Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void fullOrderWorkflow() {
        var customer = backend.customers.create(new CustomerForm("ACME AG", "info@acme.ch", "Zürich", null));
        var product = backend.products.create(new ProductForm("lap-001", "Laptop", new BigDecimal("1499.90"), null));

        OrderDto order = backend.orders.create(new CreateOrderRequest(customer.id()));
        order = backend.orders.addLine(order.id(), new AddLineRequest(product.id(), 2));
        order = backend.orders.place(order.id());

        assertThat(order.status()).isEqualTo("PLACED");
        assertThat(order.customerName()).isEqualTo("ACME AG");
        assertThat(order.total()).isEqualByComparingTo("2999.80");
        assertThat(backend.dashboard.overview().revenue()).isEqualByComparingTo("2999.80");
        assertThat(backend.dashboard.overview().ordersByStatus()).containsEntry("PLACED", 1L);
    }

    @Test
    void emailMustBeUnique() {
        backend.customers.create(new CustomerForm("A", "a@example.com", "", null));
        assertThatThrownBy(() -> backend.customers.create(new CustomerForm("B", "A@example.com", "", null)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void customerWithOrdersCannotBeDeleted() {
        var customer = backend.customers.create(new CustomerForm("A", "a@example.com", "", null));
        backend.orders.create(new CreateOrderRequest(customer.id()));

        assertThatThrownBy(() -> backend.customers.delete(customer.id()))
                .hasMessageContaining("deaktiviert");
    }

    @Test
    void inactiveCustomerCannotOrder() {
        var customer = backend.customers.create(new CustomerForm("A", "a@example.com", "", null));
        backend.customers.update(customer.id(), new CustomerForm("A", "a@example.com", "", false));

        assertThatThrownBy(() -> backend.orders.create(new CreateOrderRequest(customer.id())))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void reportsUnknownAndInvalidIds() {
        assertThatThrownBy(() -> backend.orders.get(UUID.randomUUID().toString()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> backend.orders.get("nonsense"))
                .isInstanceOf(DomainException.class);
    }
}
