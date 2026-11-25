package com.example.orders.persistence.jpa;

import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.EmailAddress;
import com.example.orders.persistence.jpa.entity.CustomerEntity;
import com.example.orders.repository.CustomerRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/** Muss innerhalb einer Transaktion aufgerufen werden (siehe UnitOfWork). */
public class JpaCustomerRepository implements CustomerRepository {

    private final EntityManager em;

    public JpaCustomerRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity existing = em.find(CustomerEntity.class, customer.getId().value());
        CustomerEntity e = existing != null ? existing : new CustomerEntity();
        e.id = customer.getId().value();
        e.name = customer.getName();
        e.email = customer.getEmail().value();
        e.city = customer.getCity();
        e.active = customer.isActive();
        if (existing == null) {
            em.persist(e);
        }
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.ofNullable(em.find(CustomerEntity.class, id.value())).map(JpaCustomerRepository::toModel);
    }

    @Override
    public Optional<Customer> findByEmail(EmailAddress email) {
        return em.createQuery("select c from CustomerEntity c where c.email = :email", CustomerEntity.class)
                .setParameter("email", email.value())
                .getResultStream().findFirst().map(JpaCustomerRepository::toModel);
    }

    @Override
    public List<Customer> findAll() {
        return em.createQuery("select c from CustomerEntity c", CustomerEntity.class)
                .getResultStream().map(JpaCustomerRepository::toModel).toList();
    }

    @Override
    public void delete(CustomerId id) {
        CustomerEntity e = em.find(CustomerEntity.class, id.value());
        if (e != null) {
            em.remove(e);
        }
    }

    @Override
    public long count() {
        return em.createQuery("select count(c) from CustomerEntity c", Long.class).getSingleResult();
    }

    private static Customer toModel(CustomerEntity e) {
        return new Customer(new CustomerId(e.id), e.name, new EmailAddress(e.email), e.city, e.active);
    }
}
