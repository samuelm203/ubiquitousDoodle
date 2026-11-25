package com.example.orders.app.config;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Legt beim ersten Start Beispieldaten an, über dieselben Controller wie die Views. */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
class DemoData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final Backend backend;

    DemoData(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!backend.customers.list().isEmpty()) {
            return;
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

        log.info("Demo-Daten angelegt: {} Kunden, {} Produkte, 3 Bestellungen", c.size(), p.size());
    }
}
