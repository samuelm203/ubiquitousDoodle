package com.example.orders.app.config;

import com.example.orders.Backend;
import com.example.orders.controller.CustomerController;
import com.example.orders.controller.DashboardController;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.ProductController;
import com.example.orders.persistence.jpa.JpaCustomerRepository;
import com.example.orders.persistence.jpa.JpaOrderRepository;
import com.example.orders.persistence.jpa.JpaProductRepository;
import com.example.orders.service.UnitOfWork;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.function.Supplier;

/**
 * Die einzige Stelle, an der Spring das Backend kennt. Für ein anderes Framework schreibt man
 * genau diese Klasse neu, z. B. als CDI-Producer in Quarkus oder als @Factory in Micronaut.
 */
@Configuration(proxyBeanMethods = false)
public class BackendConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Spring-Transaktionen hinter der framework-neutralen UnitOfWork. */
    @Bean
    UnitOfWork unitOfWork(PlatformTransactionManager txManager) {
        TransactionTemplate template = new TransactionTemplate(txManager);
        return new UnitOfWork() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }

    @Bean
    Backend backend(EntityManagerFactory emf, UnitOfWork tx, Clock clock) {
        EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        return new Backend(new JpaCustomerRepository(em), new JpaProductRepository(em),
                new JpaOrderRepository(em), tx, clock);
    }

    @Bean
    CustomerController customerController(Backend backend) {
        return backend.customers;
    }

    @Bean
    ProductController productController(Backend backend) {
        return backend.products;
    }

    @Bean
    OrderController orderController(Backend backend) {
        return backend.orders;
    }

    @Bean
    DashboardController dashboardController(Backend backend) {
        return backend.dashboard;
    }
}
