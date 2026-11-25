package com.example.orders.persistence.files;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Eine .properties-Datei als Folge von Zeilen. Beim Ändern bleiben Kommentare, Leerzeilen, Reihenfolge
 * und Zeilenumbruch-Stil erhalten; nur geänderte Einträge werden neu geschrieben.
 * Die Bedeutung eines Eintrags (Trenner, Escapes, Fortsetzungszeilen) liest {@link Properties} selbst.
 */
final class PropertiesDocument {

    private sealed interface Segment permits Raw, Entry {}

    private record Raw(String line) implements Segment {}

    private record Entry(String key, String value, List<String> lines) implements Segment {}

    private final List<Segment> segments;
    private final String newline;
    private final boolean endsWithNewline;

    private PropertiesDocument(List<Segment> segments, String newline, boolean endsWithNewline) {
        this.segments = segments;
        this.newline = newline;
        this.endsWithNewline = endsWithNewline;
    }

    static PropertiesDocument parse(String text) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        boolean trailing = text.endsWith("\n");
        List<String> lines = new ArrayList<>();
        if (!text.isEmpty()) {
            lines.addAll(List.of(text.split("\r?\n", -1)));
            if (trailing) {
                lines.remove(lines.size() - 1);
            }
        }

        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String stripped = line.stripLeading();
            if (stripped.isEmpty() || stripped.startsWith("#") || stripped.startsWith("!")) {
                segments.add(new Raw(line));
                continue;
            }
            List<String> group = new ArrayList<>(List.of(line));
            while (endsWithOddBackslashes(group.get(group.size() - 1)) && i + 1 < lines.size()) {
                group.add(lines.get(++i));
            }
            segments.add(parseEntry(group));
        }
        return new PropertiesDocument(segments, newline, trailing || text.isEmpty());
    }

    /** Alle Einträge in Dateireihenfolge (bei doppelten Schlüsseln gewinnt der letzte, wie bei Properties). */
    Map<String, String> values() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Segment s : segments) {
            if (s instanceof Entry e) {
                result.put(e.key(), e.value());
            }
        }
        return result;
    }

    /**
     * Erzeugt den Dateiinhalt für die gewünschten Werte: unveränderte Zeilen bleiben wörtlich erhalten,
     * geänderte werden ersetzt, entfernte Schlüssel gelöscht und neue am Ende angehängt.
     */
    String apply(Map<String, String> target) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Segment s : segments) {
            if (s instanceof Raw r) {
                out.add(r.line());
            } else if (s instanceof Entry e) {
                String wanted = target.get(e.key());
                if (wanted == null || !seen.add(e.key())) {
                    continue; // entfernt (oder doppelter Schlüssel: nur das erste Vorkommen behalten)
                }
                if (wanted.equals(e.value())) {
                    out.addAll(e.lines());
                } else {
                    out.add(render(e.key(), wanted));
                }
            }
        }
        target.forEach((key, value) -> {
            if (!seen.contains(key)) {
                out.add(render(key, value));
            }
        });
        return String.join(newline, out) + (out.isEmpty() || !endsWithNewline ? "" : newline);
    }

    static String render(String key, String value) {
        return escape(key, true) + "=" + escape(value, false);
    }

    static String escape(String s, boolean key) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\f' -> b.append("\\f");
                case ' ' -> b.append(key || i == 0 ? "\\ " : " ");
                case '=', ':', '#', '!' -> b.append(key ? "\\" : "").append(c);
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }

    private static Segment parseEntry(List<String> group) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(String.join("\n", group)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (p.size() != 1) {
            return new Raw(String.join("\n", group)); // nicht interpretierbar: unverändert durchreichen
        }
        String key = p.stringPropertyNames().iterator().next();
        return new Entry(key, p.getProperty(key), List.copyOf(group));
    }

    private static boolean endsWithOddBackslashes(String line) {
        int n = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            n++;
        }
        return n % 2 == 1;
    }
}
