package com.example.orders.jsf;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.DashboardDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Named
@RequestScoped
public class DashboardBean {

    /** Reihenfolge der Status-Balken. */
    private static final List<String> STATUSES = List.of("DRAFT", "PLACED", "SHIPPED", "CANCELLED");

    @Inject
    Backend backend;

    private DashboardDto data;

    public DashboardDto getData() {
        if (data == null) {
            data = backend.dashboard.overview();
        }
        return data;
    }

    public List<String> getStatuses() {
        return STATUSES;
    }

    public long count(String status) {
        Map<String, Long> byStatus = getData().ordersByStatus();
        return byStatus.getOrDefault(status, 0L);
    }

    public long getTotalOrders() {
        return STATUSES.stream().mapToLong(this::count).sum();
    }

    /** Anteil in Prozent für die Balkenbreite. */
    public int percent(String status) {
        long total = getTotalOrders();
        return total == 0 ? 0 : (int) Math.round(count(status) * 100.0 / total);
    }

    public List<OrderDto> getRecent() {
        return backend.orders.list().stream()
                .sorted(Comparator.comparing(OrderDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
    }
}
