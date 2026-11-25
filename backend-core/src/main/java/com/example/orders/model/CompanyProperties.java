package com.example.orders.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Model "Konfiguration einer Firma": mehrere .properties-Dateien (die "Spalten", z. B. dev/test/staging/prod)
 * mit denselben Schlüsseln. Fachliche Regeln (gültige Schlüssel, bekannte Spalten) stehen hier,
 * das Lesen und Schreiben der Dateien übernimmt ein Repository.
 */
public final class CompanyProperties {

    /** Firmencode = Ordnername. Bewusst streng, damit nie ein Pfad daraus werden kann. */
    public static final Pattern COMPANY_CODE = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,63}$");
    public static final Pattern KEY = Pattern.compile("^[A-Za-z0-9_.\\-\\[\\]/@]{1,200}$");
    public static final int MAX_VALUE_LENGTH = 10_000;

    /** Änderung eines Werts. {@code value == null} entfernt den Schlüssel aus dieser Spalte. */
    public record Change(String column, String key, String value) {}

    private final String company;
    private final Map<String, Map<String, String>> columns = new LinkedHashMap<>();

    public CompanyProperties(String company, Map<String, ? extends Map<String, String>> columns) {
        this.company = requireValidCompany(company);
        columns.forEach((name, values) -> this.columns.put(name, new LinkedHashMap<>(values)));
    }

    public static String requireValidCompany(String code) {
        if (code == null || !COMPANY_CODE.matcher(code).matches()) {
            throw new DomainException("Ungültiger Firmencode: " + code);
        }
        return code;
    }

    public String getCompany() {
        return company;
    }

    public List<String> getColumns() {
        return List.copyOf(columns.keySet());
    }

    /** Alle Werte einer Spalte (Schlüssel -> Wert), nur lesbar. */
    public Map<String, String> values(String column) {
        return Collections.unmodifiableMap(requireColumn(column));
    }

    /** Wert oder {@code null}, wenn der Schlüssel in dieser Spalte fehlt. */
    public String value(String column, String key) {
        return requireColumn(column).get(key);
    }

    /** Vereinigung aller Schlüssel, alphabetisch. */
    public SortedSet<String> keys() {
        SortedSet<String> all = new TreeSet<>();
        columns.values().forEach(m -> all.addAll(m.keySet()));
        return all;
    }

    /** Ein Schlüssel "unterscheidet sich", wenn er in einer Spalte fehlt oder die Werte nicht gleich sind. */
    public boolean differs(String key) {
        if (columns.size() < 2) {
            return false;
        }
        String first = null;
        boolean firstSet = false;
        for (Map<String, String> m : columns.values()) {
            String v = m.get(key);
            if (v == null) {
                return true;
            }
            if (!firstSet) {
                first = v;
                firstSet = true;
            } else if (!first.equals(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wendet Änderungen an: alles oder nichts. Zuerst werden alle geprüft, erst dann wird etwas verändert.
     *
     * @return Anzahl der Werte, die sich tatsächlich geändert haben
     */
    public int apply(List<Change> changes) {
        for (Change c : changes) {
            requireColumn(c.column());
            if (c.key() == null || !KEY.matcher(c.key()).matches()) {
                throw new DomainException("Ungültiger Schlüssel '" + c.key()
                        + "' (erlaubt: A-Z a-z 0-9 _ . - [ ] / @, max. 200 Zeichen)");
            }
            if (c.value() != null && c.value().length() > MAX_VALUE_LENGTH) {
                throw new DomainException("Wert zu lang für '" + c.key() + "' (max. " + MAX_VALUE_LENGTH + " Zeichen)");
            }
        }
        int applied = 0;
        for (Change c : changes) {
            Map<String, String> column = columns.get(c.column());
            String before = column.get(c.key());
            if (c.value() == null) {
                if (column.remove(c.key()) != null) {
                    applied++;
                }
            } else if (!c.value().equals(before)) {
                column.put(c.key(), c.value());
                applied++;
            }
        }
        return applied;
    }

    /** Tiefe Kopie, damit Repositories (z. B. In-Memory) keine veränderlichen Zustände teilen. */
    public CompanyProperties copy() {
        return new CompanyProperties(company, new LinkedHashMap<>(columns));
    }

    private Map<String, String> requireColumn(String column) {
        Map<String, String> values = columns.get(column);
        if (values == null) {
            throw new DomainException("Unbekannte Spalte '" + column + "' (vorhanden: " + new ArrayList<>(columns.keySet()) + ")");
        }
        return values;
    }
}
