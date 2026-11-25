package com.example.orders.persistence.inmemory;

import com.example.orders.Backend;
import com.example.orders.service.UnitOfWork;

import java.time.Clock;
import java.util.function.Supplier;

/** Ein komplettes, lauffähiges Backend ohne Datenbank: ideal für Tests und Framework-Experimente. */
public final class InMemoryBackend {

    private InMemoryBackend() {
    }

    /** Keine echten Transaktionen. Synchronisiert, damit gleichzeitige Requests konsistent bleiben. */
    public static final UnitOfWork SYNCHRONIZED_UOW = new UnitOfWork() {
        @Override
        public synchronized <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    public static Backend create(Clock clock) {
        return new Backend(new InMemoryCustomerRepository(), new InMemoryProductRepository(),
                new InMemoryOrderRepository(), SYNCHRONIZED_UOW, clock);
    }

    public static Backend create() {
        return create(Clock.systemUTC());
    }
}
