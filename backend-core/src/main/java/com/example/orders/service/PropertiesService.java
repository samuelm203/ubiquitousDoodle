package com.example.orders.service;

import com.example.orders.model.CompanyProperties;
import com.example.orders.model.CompanyProperties.Change;
import com.example.orders.repository.PropertiesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Geschäftslogik für die Firmenkonfigurationen: Tabelle (Schlüssel x Spalten) mit Suche, Unterschieden
 * und Seiten sowie Änderungen. Dateien kennen keine Transaktionen, deshalb serialisiert eine Sperre die
 * Änderungen innerhalb dieses Prozesses (Lesen, Ändern und Speichern ohne verlorene Updates).
 */
public class PropertiesService {

    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 2_000;

    public record CompanySummary(String code, List<String> columns, int keyCount) {}

    /** Werte in derselben Reihenfolge wie die Spalten; {@code null} = Schlüssel fehlt in dieser Spalte. */
    public record Row(String key, List<String> values) {}

    public record Query(String filter, boolean onlyDifferences, int page, int size) {}

    public record TablePage(String company, List<String> columns, List<Row> rows, int total, int page, int size) {}

    private final PropertiesRepository repository;
    private final ReentrantLock lock = new ReentrantLock();

    public PropertiesService(PropertiesRepository repository) {
        this.repository = repository;
    }

    public List<CompanySummary> companies() {
        List<CompanySummary> result = new ArrayList<>();
        for (String code : repository.companyCodes()) {
            repository.find(code).ifPresent(p -> result.add(new CompanySummary(code, p.getColumns(), p.keys().size())));
        }
        return result;
    }

    public TablePage table(String company, Query query) {
        CompanyProperties props = load(company);
        String filter = query.filter() == null ? "" : query.filter().strip().toLowerCase(Locale.ROOT);
        int size = query.size() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(query.size(), MAX_PAGE_SIZE);
        int page = Math.max(query.page(), 0);

        List<String> columns = props.getColumns();
        List<Row> matching = new ArrayList<>();
        for (String key : props.keys()) {
            if (query.onlyDifferences() && !props.differs(key)) {
                continue;
            }
            List<String> values = new ArrayList<>(columns.size());
            for (String column : columns) {
                values.add(props.value(column, key));
            }
            if (!filter.isEmpty() && !matches(key, values, filter)) {
                continue;
            }
            matching.add(new Row(key, values));
        }

        int from = (int) Math.min((long) page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        return new TablePage(company, columns, List.copyOf(matching.subList(from, to)), matching.size(), page, size);
    }

    /** @return Anzahl der Werte, die sich geändert haben (0 = es wurde nichts geschrieben) */
    public int change(String company, List<Change> changes) {
        lock.lock();
        try {
            CompanyProperties props = load(company);
            int applied = props.apply(changes);
            if (applied > 0) {
                repository.save(props);
            }
            return applied;
        } finally {
            lock.unlock();
        }
    }

    private CompanyProperties load(String company) {
        CompanyProperties.requireValidCompany(company);
        return repository.find(company).orElseThrow(() -> new NotFoundException("Firma", company));
    }

    private static boolean matches(String key, List<String> values, String filter) {
        if (key.toLowerCase(Locale.ROOT).contains(filter)) {
            return true;
        }
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(filter)) {
                return true;
            }
        }
        return false;
    }
}
