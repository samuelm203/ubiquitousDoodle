package com.example.orders.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    private final Product laptop = Product.create(new Sku("LAP-001"), "Laptop", Money.of("1200.00"));
    private final Product mouse = Product.create(new Sku("MOU-001"), "Maus", Money.of("25.50"));

    @Test
    void sumsLinesAndMergesSameProduct() {
        Order order = Order.draft(CustomerId.newId(), NOW);
        order.addProduct(laptop, 1);
        order.addProduct(mouse, 2);
        order.addProduct(mouse, 1);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.total()).isEqualTo(Money.of("1276.50"));
    }

    @Test
    void freezesPriceAtTimeOfAdding() {
        Order order = Order.draft(CustomerId.newId(), NOW);
        order.addProduct(laptop, 1);
        laptop.changePrice(Money.of("999.00"));

        assertThat(order.total()).isEqualTo(Money.of("1200.00"));
    }

    @Test
    void emptyOrderCannotBePlaced() {
        Order order = Order.draft(CustomerId.newId(), NOW);
        assertThatThrownBy(() -> order.place(NOW)).isInstanceOf(DomainException.class);
    }

    @Test
    void followsLifecycle() {
        Order order = Order.draft(CustomerId.newId(), NOW);
        order.addProduct(laptop, 1);
        order.place(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getPlacedAt()).contains(NOW);
        assertThatThrownBy(() -> order.addProduct(mouse, 1)).isInstanceOf(DomainException.class);

        order.ship();
        assertThatThrownBy(order::cancel).isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsDiscontinuedProducts() {
        mouse.setActive(false);
        Order order = Order.draft(CustomerId.newId(), NOW);
        assertThatThrownBy(() -> order.addProduct(mouse, 1)).hasMessageContaining("nicht mehr lieferbar");
    }

    @Test
    void validatesValueObjects() {
        assertThatThrownBy(() -> new EmailAddress("kein-at")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Sku("a")).isInstanceOf(DomainException.class);
        assertThat(new Sku(" lap-002 ").value()).isEqualTo("LAP-002");
        assertThat(new EmailAddress(" Max@Example.COM ").value()).isEqualTo("max@example.com");
    }
}
