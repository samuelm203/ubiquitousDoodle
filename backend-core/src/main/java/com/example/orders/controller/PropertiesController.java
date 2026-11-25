package com.example.orders.controller;

import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeResultDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyRowDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import com.example.orders.model.CompanyProperties.Change;
import com.example.orders.model.DomainException;
import com.example.orders.service.PropertiesService;
import com.example.orders.service.PropertiesService.Query;
import com.example.orders.service.PropertiesService.TablePage;

import java.util.List;

/** Controller für die Firmenkonfiguration. Framework-frei, wie die übrigen Controller. */
public class PropertiesController {

    private final PropertiesService service;

    public PropertiesController(PropertiesService service) {
        this.service = service;
    }

    public List<CompanyDto> companies() {
        return service.companies().stream()
                .map(c -> new CompanyDto(c.code(), c.columns(), c.keyCount()))
                .toList();
    }

    /** Alle Parameter sind optional: Seite 0, {@link PropertiesService#DEFAULT_PAGE_SIZE} Zeilen, kein Filter. */
    public PropertyTableDto table(String company, String filter, Boolean onlyDifferences, Integer page, Integer size) {
        TablePage t = service.table(company, new Query(filter, Boolean.TRUE.equals(onlyDifferences),
                page == null ? 0 : page, size == null ? 0 : size));
        return new PropertyTableDto(t.company(), t.columns(),
                t.rows().stream().map(r -> new PropertyRowDto(r.key(), r.values())).toList(),
                t.total(), t.page(), t.size());
    }

    public PropertyChangeResultDto change(String company, PropertyChangeRequest request) {
        if (request == null || request.changes() == null || request.changes().isEmpty()) {
            throw new DomainException("Keine Änderungen angegeben");
        }
        List<Change> changes = request.changes().stream()
                .map(c -> new Change(c.column(), c.key(), c.value()))
                .toList();
        return new PropertyChangeResultDto(service.change(company, changes));
    }
}
