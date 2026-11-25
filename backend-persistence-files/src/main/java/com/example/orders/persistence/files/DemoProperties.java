package com.example.orders.persistence.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Erzeugt Beispieldateien: 5 Firmen x 4 Umgebungen, je ca. 1000 Schlüssel. Die Umgebungen unterscheiden sich
 * absichtlich in einigen Werten, und einzelne Schlüssel fehlen (zum Ausprobieren des Vergleichs).
 */
public final class DemoProperties {

    public static final Map<String, String> COMPANIES = Map.of(
            "helvetia-logistik", "Helvetia Logistik AG",
            "alpenblick-hotels", "Alpenblick Hotels",
            "mueller-partner", "Müller & Partner",
            "seeland-bau", "Seeland Bau GmbH",
            "zuercher-treuhand", "Zürcher Treuhand AG");

    public static final List<String> ENVIRONMENTS = List.of("dev", "test", "staging", "prod");

    /** 4 Schlüssel pro Dienst, also 250 Dienste = ca. 1000 Schlüssel pro Datei. */
    public static final int SERVICES = 250;

    private DemoProperties() {
    }

    /** Legt die Dateien nur an, wenn das Verzeichnis noch keine Firmenordner enthält. */
    public static boolean generateIfEmpty(Path root) {
        try {
            if (Files.isDirectory(root)) {
                try (Stream<Path> existing = Files.list(root)) {
                    if (existing.anyMatch(Files::isDirectory)) {
                        return false;
                    }
                }
            }
            int companyIndex = 0;
            for (String code : COMPANIES.keySet().stream().sorted().toList()) {
                Path dir = Files.createDirectories(root.resolve(code));
                for (int env = 0; env < ENVIRONMENTS.size(); env++) {
                    Files.writeString(dir.resolve(ENVIRONMENTS.get(env) + ".properties"),
                            content(code, COMPANIES.get(code), companyIndex, env), StandardCharsets.UTF_8);
                }
                companyIndex++;
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String content(String code, String name, int companyIndex, int envIndex) {
        String env = ENVIRONMENTS.get(envIndex);
        boolean prod = "prod".equals(env);
        String host = prod ? code + ".example.ch" : code + "-" + env + ".example.ch";

        StringBuilder b = new StringBuilder();
        b.append("# ").append(name).append(" - Umgebung ").append(env).append('\n');
        b.append("# Beispiel-Konfiguration (generiert)\n");
        for (int s = 0; s < SERVICES; s++) {
            if (s % 50 == 0) {
                b.append(String.format("\n# Dienste s%03d - s%03d\n", s, Math.min(s + 49, SERVICES - 1)));
            }
            int timeout = 1000 + (s % 9) * 500 + companyIndex * 100 + (prod && s % 5 == 0 ? 2000 : 0);
            boolean enabled = !(envIndex == 0 && s % 11 == 0);
            int retries = (envIndex >= 2 && s % 4 == 0) ? 5 : 3;

            b.append(String.format("service.s%03d.url=https://%s/api/s%03d\n", s, host, s));
            b.append(String.format("service.s%03d.timeout-ms=%d\n", s, timeout));
            b.append(String.format("service.s%03d.enabled=%b\n", s, enabled));
            if (!(envIndex == 0 && s % 97 == 0)) { // in dev fehlen einzelne Schlüssel
                b.append(String.format("service.s%03d.retries=%d\n", s, retries));
            }
        }
        return b.toString();
    }
}
