package com.example.orders;

import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;

import java.math.BigDecimal;
import java.util.List;

/**
 * Beispieldaten für jede Laufzeit. Geht nur über die Controller, wie jede View.
 * Jede App ruft {@code DemoData.seed(backend)} einmal beim Start auf.
 */
public final class DemoData {

    private DemoData() {
    }

    /** Legt Beispieldaten an, falls noch keine Kunden existieren. @return true, wenn Daten angelegt wurden. */
    public static boolean seed(Backend backend) {
        if (!backend.customers.list().isEmpty()) {
            return false;
        }
        List<CustomerDto> c = List.of(
                backend.customers.create(new CustomerForm("Helvetia Logistik AG", "einkauf@helvetia-logistik.example", "Basel", null)),
                backend.customers.create(new CustomerForm("Alpenblick Hotels", "office@alpenblick.example", "Interlaken", null)),
                backend.customers.create(new CustomerForm("Müller & Partner", "info@mueller-partner.example", "Zürich", null)),
                backend.customers.create(new CustomerForm("Seeland Bau GmbH", "kontakt@seeland-bau.example", "Biel", null)));
        List<ProductDto> p = List.of(
                backend.products.create(new ProductForm("LAP-14", "Business-Laptop 14\"", new BigDecimal("1490.00"), null)),
                backend.products.create(new ProductForm("MON-27", "Monitor 27\" 4K", new BigDecimal("429.00"), null)),
                backend.products.create(new ProductForm("DOCK-USB-C", "USB-C Dockingstation", new BigDecimal("189.90"), null)),
                backend.products.create(new ProductForm("HEAD-ANC", "Headset mit ANC", new BigDecimal("219.00"), null)),
                backend.products.create(new ProductForm("SUP-1Y", "Support-Paket 1 Jahr", new BigDecimal("350.00"), null)));

        var o1 = backend.orders.create(new CreateOrderRequest(c.get(0).id()));
        backend.orders.addLine(o1.id(), new AddLineRequest(p.get(0).id(), 10));
        backend.orders.addLine(o1.id(), new AddLineRequest(p.get(2).id(), 10));
        backend.orders.place(o1.id());
        backend.orders.ship(o1.id());

        var o2 = backend.orders.create(new CreateOrderRequest(c.get(1).id()));
        backend.orders.addLine(o2.id(), new AddLineRequest(p.get(1).id(), 4));
        backend.orders.addLine(o2.id(), new AddLineRequest(p.get(4).id(), 1));
        backend.orders.place(o2.id());

        var o3 = backend.orders.create(new CreateOrderRequest(c.get(2).id()));
        backend.orders.addLine(o3.id(), new AddLineRequest(p.get(3).id(), 6));
        return true;
    }
}
