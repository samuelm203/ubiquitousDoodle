package com.example.orders.controller;

import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.EmailAddress;
import com.example.orders.service.CustomerService;

import java.util.List;

/**
 * Controller im MVC-Sinn: nimmt Eingaben der View (Formulare, IDs als String) entgegen,
 * ruft das Model über die Services auf und gibt View-taugliche DTOs zurück.
 * Framework-frei: wird von der Vaadin-View direkt und von der REST-API delegiert genutzt.
 */
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    public List<CustomerDto> list() {
        return service.findAll().stream().map(CustomerController::toDto).toList();
    }

    public CustomerDto get(String id) {
        return toDto(service.get(id(id)));
    }

    public CustomerDto create(CustomerForm form) {
        return toDto(service.register(form.name(), new EmailAddress(form.email()), form.city()));
    }

    public CustomerDto update(String id, CustomerForm form) {
        boolean active = form.active() == null || form.active();
        return toDto(service.update(id(id), form.name(), new EmailAddress(form.email()), form.city(), active));
    }

    public void delete(String id) {
        service.delete(id(id));
    }

    static CustomerDto toDto(Customer c) {
        return new CustomerDto(c.getId().toString(), c.getName(), c.getEmail().value(), c.getCity(), c.isActive());
    }

    private static CustomerId id(String raw) {
        return new CustomerId(Ids.parse(raw));
    }
}
