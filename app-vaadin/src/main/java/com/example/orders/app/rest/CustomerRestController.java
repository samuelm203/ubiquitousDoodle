package com.example.orders.app.rest;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dünne HTTP-Schicht (Spring Web MVC): delegiert 1:1 an den framework-freien Controller. */
@RestController
@RequestMapping("/api/customers")
class CustomerRestController {

    private final CustomerController controller;

    CustomerRestController(CustomerController controller) {
        this.controller = controller;
    }

    @GetMapping
    List<CustomerDto> list() {
        return controller.list();
    }

    @GetMapping("/{id}")
    CustomerDto get(@PathVariable String id) {
        return controller.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CustomerDto create(@RequestBody CustomerForm form) {
        return controller.create(form);
    }

    @PutMapping("/{id}")
    CustomerDto update(@PathVariable String id, @RequestBody CustomerForm form) {
        return controller.update(id, form);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        controller.delete(id);
    }
}
