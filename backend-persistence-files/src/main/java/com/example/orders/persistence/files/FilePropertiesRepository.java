package com.example.orders.persistence.files;

import com.example.orders.model.CompanyProperties;
import com.example.orders.model.DomainException;
import com.example.orders.repository.PropertiesRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Struktur: {@code <root>/<firma>/<spalte>.properties}. Jede Datei einer Firma ist eine Spalte
 * (sortiert nach Dateiname). Dateien sind UTF-8, wie bei Spring Boot üblich.
 * Geschrieben wird über eine temporäre Datei und Umbenennen, damit nie eine halbe Datei liegen bleibt.
 */
public class FilePropertiesRepository implements PropertiesRepository {

    private static final String SUFFIX = ".properties";

    private final Path root;

    public FilePropertiesRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public List<String> companyCodes() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(root)) {
            return dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> CompanyProperties.COMPANY_CODE.matcher(name).matches())
                    .filter(name -> !propertyFiles(root.resolve(name)).isEmpty())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<CompanyProperties> find(String company) {
        List<Path> files = propertyFiles(directory(company));
        if (files.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Map<String, String>> columns = new LinkedHashMap<>();
        for (Path file : files) {
            columns.put(columnName(file), PropertiesDocument.parse(read(file)).values());
        }
        return Optional.of(new CompanyProperties(company, columns));
    }

    @Override
    public void save(CompanyProperties properties) {
        Path dir = directory(properties.getCompany());
        for (String column : properties.getColumns()) {
            Path file = dir.resolve(column + SUFFIX);
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("Datei fehlt: " + dir.getFileName() + "/" + file.getFileName());
            }
            PropertiesDocument document = PropertiesDocument.parse(read(file));
            Map<String, String> target = properties.values(column);
            if (!document.values().equals(target)) {
                write(file, document.apply(target));
            }
        }
    }

    private Path directory(String company) {
        CompanyProperties.requireValidCompany(company);
        Path dir = root.resolve(company).normalize();
        if (!dir.startsWith(root)) {
            throw new DomainException("Ungültiger Firmencode: " + company);
        }
        return dir;
    }

    private static List<Path> propertyFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(SUFFIX))
                    .filter(p -> p.getFileName().toString().length() > SUFFIX.length())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String columnName(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - SUFFIX.length());
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) {
        Path temp = null;
        try {
            temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // temporäre Datei bleibt höchstens liegen
                }
            }
        }
    }
}
