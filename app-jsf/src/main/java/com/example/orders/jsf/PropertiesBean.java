package com.example.orders.jsf;

import com.example.orders.PropertiesModule;
import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyRowDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Firmenkonfiguration als Tabelle (Zeilen = Schlüssel, Spalten = Umgebungen). Nur ADMIN.
 * Es wird immer nur eine Seite geladen; nicht gespeicherte Änderungen aller Seiten stehen in {@code pending}
 * und werden gesammelt (alles oder nichts) gespeichert. Leeres Feld = Schlüssel fehlt bzw. wird gelöscht.
 */
@Named
@ViewScoped
public class PropertiesBean implements Serializable {

    /** Eine Zeile; {@code values} ist direkt an die Eingabefelder gebunden. */
    public static class Row implements Serializable {
        private final String key;
        private final List<String> original;
        private final List<String> values;

        Row(String key, List<String> original) {
            this.key = key;
            this.original = new ArrayList<>(original);
            this.values = new ArrayList<>(original);
        }

        public String getKey() { return key; }
        public List<String> getValues() { return values; }
        public boolean changed(int i) { return !Objects.equals(original.get(i), values.get(i)); }
    }

    @Inject
    transient PropertiesModule module;
    @Inject
    RoleBean role;

    private List<CompanyDto> companies;
    private String company;
    private String filter = "";
    private boolean onlyDifferences;
    private int size = 100;
    private int page;

    private List<String> columns = List.of();
    private List<Row> rows;
    private int total;
    /** Nicht gespeicherte Änderungen über alle Seiten; Schlüssel = Spalte + Zeilenschlüssel. */
    private final Map<String, PropertyChangeDto> pending = new LinkedHashMap<>();

    public boolean isAllowed() {
        return role.isAdmin();
    }

    public List<CompanyDto> getCompanies() {
        if (companies == null) {
            companies = module.controller.companies();
            if (company == null && !companies.isEmpty()) {
                company = companies.get(0).code();
            }
        }
        return companies;
    }

    public List<Row> getRows() {
        if (rows == null) {
            getCompanies();
            load();
        }
        return rows;
    }

    private void load() {
        rows = List.of();
        if (!isAllowed() || company == null) {
            return;
        }
        PropertyTableDto t = Messages.attempt(
                () -> module.controller.table(company, filter, onlyDifferences, page, size), null);
        if (t == null) {
            return;
        }
        columns = t.columns();
        total = t.total();
        page = t.page();
        List<Row> result = new ArrayList<>(t.rows().size());
        for (PropertyRowDto r : t.rows()) {
            Row row = new Row(r.key(), r.values());
            for (int i = 0; i < columns.size(); i++) {
                PropertyChangeDto c = pending.get(pendingKey(columns.get(i), r.key()));
                if (c != null) {
                    row.values.set(i, c.value());
                }
            }
            result.add(row);
        }
        rows = result;
    }

    private static String pendingKey(String column, String key) {
        return column + '\u0000' + key;
    }

    /** Wird nach dem Ändern eines Felds aufgerufen: merkt die Änderung der Zeile oder verwirft sie. */
    public void cellChanged(Row row) {
        for (int i = 0; i < columns.size(); i++) {
            String value = row.values.get(i);
            if (value != null && value.isEmpty()) {
                value = null;
                row.values.set(i, null);
            }
            String k = pendingKey(columns.get(i), row.key);
            if (Objects.equals(value, row.original.get(i))) {
                pending.remove(k);
            } else {
                pending.put(k, new PropertyChangeDto(columns.get(i), row.key, value));
            }
        }
    }

    /** Filter, Umschalter oder Seitengröße geändert: Seite 0 neu laden. */
    public void reload() {
        page = 0;
        load();
    }

    public void companyChanged() {
        pending.clear();
        reload();
    }

    public void prev() {
        if (page > 0) {
            page--;
            load();
        }
    }

    public void next() {
        if (isHasNext()) {
            page++;
            load();
        }
    }

    public void save() {
        if (!isAllowed() || pending.isEmpty()) {
            return;
        }
        PropertyChangeRequest req = new PropertyChangeRequest(new ArrayList<>(pending.values()));
        Integer applied = Messages.attempt(() -> module.controller.change(company, req).applied(), null);
        if (applied != null) {
            Messages.info(applied + " Werte gespeichert");
            pending.clear();
            companies = null;
            load();
        }
    }

    public void discard() {
        pending.clear();
        load();
    }

    public int getPendingCount() { return pending.size(); }
    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageCount() { return Math.max(1, (total + size - 1) / size); }
    public boolean isHasPrev() { return page > 0; }
    public boolean isHasNext() { return (page + 1) * size < total; }
    public List<String> getColumns() { getRows(); return columns; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public boolean isOnlyDifferences() { return onlyDifferences; }
    public void setOnlyDifferences(boolean onlyDifferences) { this.onlyDifferences = onlyDifferences; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
