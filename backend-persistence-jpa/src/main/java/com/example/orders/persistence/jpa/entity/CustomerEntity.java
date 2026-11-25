package com.example.orders.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Persistenz-Repräsentation. Bewusst getrennt vom Model, damit das Model JPA-frei bleibt. */
@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    public UUID id;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(nullable = false, unique = true, length = 254)
    public String email;

    @Column(length = 100)
    public String city;

    @Column(nullable = false)
    public boolean active;
}
