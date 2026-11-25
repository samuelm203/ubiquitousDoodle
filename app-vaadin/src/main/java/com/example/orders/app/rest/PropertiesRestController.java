package com.example.orders.app.rest;

import com.example.orders.controller.PropertiesController;
import com.example.orders.controller.dto.PropertyDtos.CompanyDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeResultDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dünne HTTP-Schicht für die Firmenkonfiguration. Nur für ADMIN (siehe SecurityConfig): Werte können Geheimnisse sein. */
@RestController
@RequestMapping("/api/properties")
class PropertiesRestController {

    private final PropertiesController controller;

    PropertiesRestController(PropertiesController controller) {
        this.controller = controller;
    }

    @GetMapping("/companies")
    List<CompanyDto> companies() {
        return controller.companies();
    }

    /** Tabelle: eine Zeile pro Schlüssel, eine Spalte pro Datei. */
    @GetMapping("/{company}")
    PropertyTableDto table(@PathVariable String company,
                           @RequestParam(name = "filter", required = false) String filter,
                           @RequestParam(name = "onlyDifferences", required = false) Boolean onlyDifferences,
                           @RequestParam(name = "page", required = false) Integer page,
                           @RequestParam(name = "size", required = false) Integer size) {
        return controller.table(company, filter, onlyDifferences, page, size);
    }

    /** Mehrere Änderungen auf einmal; alles oder nichts. {@code value: null} löscht den Schlüssel. */
    @PutMapping("/{company}")
    PropertyChangeResultDto change(@PathVariable String company, @RequestBody PropertyChangeRequest request) {
        return controller.change(company, request);
    }
}
