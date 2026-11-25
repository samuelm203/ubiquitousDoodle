package com.example.orders.persistence.jpa;

import com.example.orders.model.CustomerId;
import com.example.orders.model.Money;
import com.example.orders.model.Order;
import com.example.orders.model.OrderId;
import com.example.orders.model.OrderLine;
import com.example.orders.model.OrderStatus;
import com.example.orders.model.ProductId;
import com.example.orders.persistence.jpa.entity.OrderEntity;
import com.example.orders.persistence.jpa.entity.OrderLineEmbeddable;
import com.example.orders.repository.OrderRepository;
import jakarta.persistence.EntityManager;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

public class JpaOrderRepository implements OrderRepository {

    private final EntityManager em;

    public JpaOrderRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Order save(Order order) {
        OrderEntity existing = em.find(OrderEntity.class, order.getId().value());
        OrderEntity e = existing != null ? existing : new OrderEntity();
        e.id = order.getId().value();
        e.customerId = order.getCustomerId().value();
        e.createdAt = order.getCreatedAt();
        e.status = order.getStatus().name();
        e.placedAt = order.getPlacedAt().orElse(null);
        e.lines.clear();
        for (OrderLine line : order.getLines()) {
            OrderLineEmbeddable l = new OrderLineEmbeddable();
            l.productId = line.productId().value();
            l.productName = line.productName();
            l.unitPrice = line.unitPrice().amount();
            l.currency = line.unitPrice().currency().getCurrencyCode();
            l.quantity = line.quantity();
            e.lines.add(l);
        }
        if (existing == null) {
            em.persist(e);
        }
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(em.find(OrderEntity.class, id.value())).map(JpaOrderRepository::toModel);
    }

    @Override
    public List<Order> findAll() {
        return em.createQuery("select distinct o from OrderEntity o left join fetch o.lines", OrderEntity.class)
                .getResultStream().map(JpaOrderRepository::toModel).toList();
    }

    @Override
    public boolean existsByCustomer(CustomerId customerId) {
        return em.createQuery("select count(o) from OrderEntity o where o.customerId = :c", Long.class)
                .setParameter("c", customerId.value())
                .getSingleResult() > 0;
    }

    private static Order toModel(OrderEntity e) {
        List<OrderLine> lines = e.lines.stream()
                .map(l -> new OrderLine(new ProductId(l.productId), l.productName,
                        new Money(l.unitPrice, Currency.getInstance(l.currency)), l.quantity))
                .toList();
        return new Order(new OrderId(e.id), new CustomerId(e.customerId), OrderStatus.valueOf(e.status),
                e.createdAt, e.placedAt, lines);
    }
}
