package com.example.orders.controller.dto;

import java.util.List;

/** Datenobjekte der Firmenkonfiguration (REST/JSON-tauglich, ohne Logik). */
public final class PropertyDtos {

    private PropertyDtos() {
    }

    public record CompanyDto(String code, List<String> columns, int keyCount) {}

    /** {@code values} ist wie {@code columns} sortiert; {@code null} = Schlüssel fehlt in dieser Spalte. */
    public record PropertyRowDto(String key, List<String> values) {}

    public record PropertyTableDto(String company, List<String> columns, List<PropertyRowDto> rows,
                                   int total, int page, int size) {}

    /** {@code value == null} entfernt den Schlüssel aus der Spalte. */
    public record PropertyChangeDto(String column, String key, String value) {}

    public record PropertyChangeRequest(List<PropertyChangeDto> changes) {}

    public record PropertyChangeResultDto(int applied) {}
}
