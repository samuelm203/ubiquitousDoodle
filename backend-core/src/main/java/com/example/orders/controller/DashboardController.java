package com.example.orders.controller;

import com.example.orders.controller.dto.Dtos.DashboardDto;
import com.example.orders.service.DashboardService;

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    public DashboardDto overview() {
        DashboardService.Stats s = service.stats();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        s.ordersByStatus().forEach((status, count) -> byStatus.put(status.name(), count));
        return new DashboardDto(s.customers(), s.products(), byStatus, s.revenue().amount(),
                s.revenue().currency().getCurrencyCode());
    }
}
