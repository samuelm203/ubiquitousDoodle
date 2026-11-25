package com.example.orders.repository;

import com.example.orders.model.CompanyProperties;

import java.util.List;
import java.util.Optional;

/** Persistenz-Schnittstelle für die Firmenkonfigurationen (Dateien, Datenbank, Speicher ...). */
public interface PropertiesRepository {

    /** Alle Firmencodes, alphabetisch. */
    List<String> companyCodes();

    Optional<CompanyProperties> find(String company);

    /** Schreibt die Werte aller Spalten zurück. Unveränderte Spalten dürfen übersprungen werden. */
    void save(CompanyProperties properties);
}
