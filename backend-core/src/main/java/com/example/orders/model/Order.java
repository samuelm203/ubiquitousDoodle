package com.example.orders.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Model "Bestellung". Änderungen laufen über Methoden, die die Geschäftsregeln prüfen.
 * Setter gibt es bewusst nicht.
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final Instant createdAt;
    private final List<OrderLine> lines;
    private OrderStatus status;
    private Instant placedAt;

    /** Vollständiger Konstruktor, z. B. zum Rekonstruieren aus der Datenbank. */
    public Order(OrderId id, CustomerId customerId, OrderStatus status, Instant createdAt,
                 Instant placedAt, List<OrderLine> lines) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.placedAt = placedAt;
        this.lines = new ArrayList<>(lines);
    }

    public static Order draft(CustomerId customerId, Instant now) {
        return new Order(OrderId.newId(), customerId, OrderStatus.DRAFT, now, null, List.of());
    }

    public void addProduct(Product product, int quantity) {
        requireDraft();
        if (!product.isActive()) {
            throw new DomainException("Produkt '" + product.getName() + "' ist nicht mehr lieferbar");
        }
        Optional<OrderLine> existing = findLine(product.getId());
        if (existing.isPresent()) {
            OrderLine line = existing.get();
            lines.set(lines.indexOf(line), line.withQuantity(line.quantity() + quantity));
        } else {
            lines.add(new OrderLine(product.getId(), product.getName(), product.getPrice(), quantity));
        }
    }

    public void removeProduct(ProductId productId) {
        requireDraft();
        lines.removeIf(l -> l.productId().equals(productId));
    }

    public void place(Instant now) {
        if (lines.isEmpty()) {
            throw new DomainException("Eine leere Bestellung kann nicht aufgegeben werden");
        }
        transitionTo(OrderStatus.PLACED);
        this.placedAt = now;
    }

    public void ship() {
        transitionTo(OrderStatus.SHIPPED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public Money total() {
        return lines.stream().map(OrderLine::total).reduce(Money.zero(), Money::plus);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new DomainException("Statuswechsel " + status + " -> " + target + " ist nicht erlaubt");
        }
        this.status = target;
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("Positionen können nur in Entwürfen geändert werden");
        }
    }

    private Optional<OrderLine> findLine(ProductId productId) {
        return lines.stream().filter(l -> l.productId().equals(productId)).findFirst();
    }

    public OrderId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Optional<Instant> getPlacedAt() { return Optional.ofNullable(placedAt); }
    public List<OrderLine> getLines() { return List.copyOf(lines); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Order other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
