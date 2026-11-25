package com.example.orders.persistence.inmemory;

import com.example.orders.model.CompanyProperties;
import com.example.orders.repository.PropertiesRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Hält die Konfigurationen im Speicher. Gibt Kopien heraus, damit Änderungen erst über {@code save} wirken. */
public class InMemoryPropertiesRepository implements PropertiesRepository {

    private final Map<String, CompanyProperties> store = new ConcurrentHashMap<>();

    @Override
    public List<String> companyCodes() {
        return store.keySet().stream().sorted().toList();
    }

    @Override
    public Optional<CompanyProperties> find(String company) {
        return Optional.ofNullable(store.get(company)).map(CompanyProperties::copy);
    }

    @Override
    public void save(CompanyProperties properties) {
        store.put(properties.getCompany(), properties.copy());
    }
}
