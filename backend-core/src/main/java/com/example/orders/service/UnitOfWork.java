package com.example.orders.service;

import java.util.function.Supplier;

/**
 * Transaktionsgrenze, ohne an ein Framework gebunden zu sein.
 * Spring: TransactionTemplate, Quarkus: QuarkusTransaction, Jakarta EE: UserTransaction,
 * In-Memory: einfach ausführen.
 */
public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> work);

    default void run(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
