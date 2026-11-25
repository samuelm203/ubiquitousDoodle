package com.example.orders.controller;

import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import com.example.orders.model.DomainException;
import com.example.orders.model.Money;
import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.model.Sku;
import com.example.orders.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    public List<ProductDto> list() {
        return service.findAll().stream().map(ProductController::toDto).toList();
    }

    public ProductDto get(String id) {
        return toDto(service.get(id(id)));
    }

    public ProductDto create(ProductForm form) {
        return toDto(service.create(new Sku(form.sku()), form.name(), price(form.price())));
    }

    public ProductDto update(String id, ProductForm form) {
        boolean active = form.active() == null || form.active();
        return toDto(service.update(id(id), form.name(), price(form.price()), active));
    }

    static ProductDto toDto(Product p) {
        return new ProductDto(p.getId().toString(), p.getSku().value(), p.getName(), p.getPrice().amount(),
                p.getPrice().currency().getCurrencyCode(), p.isActive());
    }

    private static Money price(BigDecimal amount) {
        if (amount == null) {
            throw new DomainException("Preis fehlt");
        }
        return Money.of(amount);
    }

    private static ProductId id(String raw) {
        return new ProductId(Ids.parse(raw));
    }
}
