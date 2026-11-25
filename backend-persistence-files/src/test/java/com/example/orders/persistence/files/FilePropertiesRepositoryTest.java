package com.example.orders.persistence.files;

import com.example.orders.model.CompanyProperties;
import com.example.orders.model.CompanyProperties.Change;
import com.example.orders.model.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePropertiesRepositoryTest {

    @TempDir
    Path root;

    FilePropertiesRepository repository;

    @BeforeEach
    void setUp() {
        assertThat(DemoProperties.generateIfEmpty(root)).isTrue();
        repository = new FilePropertiesRepository(root);
    }

    @Test
    void demoDataHasFiveCompaniesWithFourColumnsAndAboutAThousandKeys() {
        assertThat(repository.companyCodes()).hasSize(5);

        CompanyProperties p = repository.find("seeland-bau").orElseThrow();
        assertThat(p.getColumns()).containsExactly("dev", "prod", "staging", "test");
        assertThat(p.keys()).hasSizeBetween(990, 1010);
        assertThat(DemoProperties.generateIfEmpty(root)).as("zweites Mal nichts überschreiben").isFalse();
    }

    @Test
    void demoDataContainsDifferencesAndMissingKeys() {
        CompanyProperties p = repository.find("seeland-bau").orElseThrow();

        assertThat(p.keys().stream().filter(p::differs).count()).isPositive();
        assertThat(p.value("dev", "service.s000.retries")).isNull(); // fehlt absichtlich in dev
        assertThat(p.value("prod", "service.s000.retries")).isEqualTo("5");
    }

    @Test
    void savingChangesOnlyTheTouchedValueAndKeepsComments() throws IOException {
        Path file = root.resolve("seeland-bau/prod.properties");
        String before = Files.readString(file, StandardCharsets.UTF_8);

        CompanyProperties p = repository.find("seeland-bau").orElseThrow();
        p.apply(List.of(new Change("prod", "service.s001.timeout-ms", "42")));
        repository.save(p);

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(after).contains("service.s001.timeout-ms=42").startsWith("# Seeland Bau GmbH - Umgebung prod");
        assertThat(after.lines().count()).isEqualTo(before.lines().count());
        assertThat(repository.find("seeland-bau").orElseThrow().value("prod", "service.s001.timeout-ms")).isEqualTo("42");
        // andere Dateien bleiben unberührt
        assertThat(repository.find("seeland-bau").orElseThrow().value("test", "service.s001.timeout-ms")).isNotEqualTo("42");
    }

    @Test
    void savingWithoutChangesDoesNotRewriteFiles() throws IOException {
        Path file = root.resolve("seeland-bau/dev.properties");
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(1_000_000));

        repository.save(repository.find("seeland-bau").orElseThrow());

        assertThat(Files.getLastModifiedTime(file).toMillis()).isEqualTo(1_000_000);
    }

    @Test
    void leavesNoTemporaryFilesBehind() throws IOException {
        CompanyProperties p = repository.find("seeland-bau").orElseThrow();
        p.apply(List.of(new Change("dev", "neuer.schluessel", "x")));
        repository.save(p);

        try (Stream<Path> files = Files.list(root.resolve("seeland-bau"))) {
            assertThat(files.map(f -> f.getFileName().toString())).allMatch(n -> n.endsWith(".properties"));
        }
    }

    @Test
    void rejectsPathTraversalAndUnknownCompanies() {
        assertThatThrownBy(() -> repository.find("../seeland-bau")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> repository.find("..")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> repository.find("a/b")).isInstanceOf(DomainException.class);
        assertThat(repository.find("gibt-es-nicht")).isEmpty();
    }
}
