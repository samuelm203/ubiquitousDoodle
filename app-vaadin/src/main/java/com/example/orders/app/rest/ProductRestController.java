package com.example.orders.app.rest;

import com.example.orders.controller.ProductController;
import com.example.orders.controller.dto.Dtos.ProductDto;
import com.example.orders.controller.dto.Dtos.ProductForm;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
class ProductRestController {

    private final ProductController controller;

    ProductRestController(ProductController controller) {
        this.controller = controller;
    }

    @GetMapping
    List<ProductDto> list() {
        return controller.list();
    }

    @GetMapping("/{id}")
    ProductDto get(@PathVariable String id) {
        return controller.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductDto create(@RequestBody ProductForm form) {
        return controller.create(form);
    }

    @PutMapping("/{id}")
    ProductDto update(@PathVariable String id, @RequestBody ProductForm form) {
        return controller.update(id, form);
    }
}
