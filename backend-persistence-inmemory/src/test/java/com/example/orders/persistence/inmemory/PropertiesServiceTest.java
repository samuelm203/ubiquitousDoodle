package com.example.orders.persistence.inmemory;

import com.example.orders.PropertiesModule;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeDto;
import com.example.orders.controller.dto.PropertyDtos.PropertyChangeRequest;
import com.example.orders.controller.dto.PropertyDtos.PropertyTableDto;
import com.example.orders.model.CompanyProperties;
import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Controller -> Service -> Model -> Repository für die Firmenkonfiguration, ohne Framework und ohne Dateien. */
class PropertiesServiceTest {

    InMemoryPropertiesRepository repository = new InMemoryPropertiesRepository();
    PropertiesModule module = new PropertiesModule(repository);

    @BeforeEach
    void setUp() {
        Map<String, Map<String, String>> columns = new LinkedHashMap<>();
        columns.put("dev", new LinkedHashMap<>(Map.of("db.url", "jdbc:dev", "mode", "x", "only.dev", "1")));
        columns.put("prod", new LinkedHashMap<>(Map.of("db.url", "jdbc:prod", "mode", "x")));
        repository.save(new CompanyProperties("acme", columns));
    }

    @Test
    void listsCompaniesWithColumnsAndKeyCount() {
        var companies = module.controller.companies();

        assertThat(companies).hasSize(1);
        assertThat(companies.get(0).code()).isEqualTo("acme");
        assertThat(companies.get(0).columns()).containsExactly("dev", "prod");
        assertThat(companies.get(0).keyCount()).isEqualTo(3);
    }

    @Test
    void tableHasOneRowPerKeyWithNullForMissingValues() {
        PropertyTableDto t = module.controller.table("acme", null, null, null, null);

        assertThat(t.columns()).containsExactly("dev", "prod");
        assertThat(t.rows()).extracting("key").containsExactly("db.url", "mode", "only.dev");
        assertThat(t.rows().get(2).values()).containsExactly("1", null);
    }

    @Test
    void filtersByKeyOrValueAndShowsOnlyDifferences() {
        assertThat(module.controller.table("acme", "JDBC:PROD", null, null, null).rows()).extracting("key")
                .containsExactly("db.url");
        assertThat(module.controller.table("acme", null, true, null, null).rows()).extracting("key")
                .containsExactly("db.url", "only.dev"); // "mode" ist überall gleich
    }

    @Test
    void pagesThroughRows() {
        PropertyTableDto page1 = module.controller.table("acme", null, null, 1, 2);

        assertThat(page1.total()).isEqualTo(3);
        assertThat(page1.rows()).extracting("key").containsExactly("only.dev");
        assertThat(module.controller.table("acme", null, null, 99, 2).rows()).isEmpty();
    }

    @Test
    void changesAddsAndRemovesValues() {
        var result = module.controller.change("acme", new PropertyChangeRequest(List.of(
                new PropertyChangeDto("prod", "db.url", "jdbc:prod2"),
                new PropertyChangeDto("prod", "new.key", "n"),
                new PropertyChangeDto("dev", "only.dev", null))));

        assertThat(result.applied()).isEqualTo(3);
        PropertyTableDto t = module.controller.table("acme", null, null, null, null);
        assertThat(t.rows()).extracting("key").containsExactly("db.url", "mode", "new.key");
        assertThat(t.rows().get(0).values()).containsExactly("jdbc:dev", "jdbc:prod2");
    }

    @Test
    void invalidChangeAppliesNothing() {
        var request = new PropertyChangeRequest(List.of(
                new PropertyChangeDto("prod", "mode", "geaendert"),
                new PropertyChangeDto("prod", "kaputt key", "x")));

        assertThatThrownBy(() -> module.controller.change("acme", request)).isInstanceOf(DomainException.class);
        assertThat(module.controller.table("acme", "geaendert", null, null, null).rows()).isEmpty();
    }

    @Test
    void rejectsUnknownColumnCompanyAndEmptyRequests() {
        assertThatThrownBy(() -> module.controller.change("acme",
                new PropertyChangeRequest(List.of(new PropertyChangeDto("staging", "a", "b")))))
                .hasMessageContaining("Unbekannte Spalte");
        assertThatThrownBy(() -> module.controller.table("gibt-es-nicht", null, null, null, null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> module.controller.table("../etc", null, null, null, null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> module.controller.change("acme", new PropertyChangeRequest(List.of())))
                .isInstanceOf(DomainException.class);
    }
}
