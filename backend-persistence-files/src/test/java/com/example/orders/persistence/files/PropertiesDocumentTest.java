package com.example.orders.persistence.files;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesDocumentTest {

    private static final String SAMPLE = """
            # Kopfzeile
            a.one=1

            ! anderer Kommentar
            a.two = zwei
            a.three:drei
            """;

    @Test
    void changingOneValueKeepsCommentsOrderAndUntouchedLines() {
        PropertiesDocument doc = PropertiesDocument.parse(SAMPLE);
        Map<String, String> target = new LinkedHashMap<>(doc.values());
        target.put("a.one", "eins");

        assertThat(doc.apply(target)).isEqualTo("""
                # Kopfzeile
                a.one=eins

                ! anderer Kommentar
                a.two = zwei
                a.three:drei
                """);
    }

    @Test
    void readsTheSameValuesAsJavaProperties() {
        assertThat(PropertiesDocument.parse(SAMPLE).values())
                .containsExactly(Map.entry("a.one", "1"), Map.entry("a.two", "zwei"), Map.entry("a.three", "drei"));
    }

    @Test
    void removesAndAppendsKeys() {
        PropertiesDocument doc = PropertiesDocument.parse(SAMPLE);
        Map<String, String> target = new LinkedHashMap<>(doc.values());
        target.remove("a.two");
        target.put("b.new", "neu");

        String result = doc.apply(target);
        assertThat(result).doesNotContain("a.two").endsWith("a.three:drei\nb.new=neu\n").contains("# Kopfzeile");
    }

    @Test
    void roundTripsSpecialCharacters() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("key.with[0]", " führendes Leerzeichen und = Gleichheitszeichen: ü € \\ Backslash");
        values.put("multi", "zeile1\nzeile2\ttab");
        values.put("hash", "#kein Kommentar");

        String text = PropertiesDocument.parse("").apply(values);

        assertThat(PropertiesDocument.parse(text).values()).isEqualTo(values);
    }

    @Test
    void handlesContinuationLinesAndKeepsThemWhenUnchanged() {
        String text = "long=eins,\\\n    zwei,\\\n    drei\nother=x\n";
        PropertiesDocument doc = PropertiesDocument.parse(text);

        assertThat(doc.values()).containsEntry("long", "eins,zwei,drei").containsEntry("other", "x");

        Map<String, String> target = new LinkedHashMap<>(doc.values());
        target.put("other", "y");
        assertThat(doc.apply(target)).isEqualTo("long=eins,\\\n    zwei,\\\n    drei\nother=y\n");
    }

    @Test
    void keepsWindowsLineEndings() {
        PropertiesDocument doc = PropertiesDocument.parse("a=1\r\nb=2\r\n");
        Map<String, String> target = new LinkedHashMap<>(doc.values());
        target.put("a", "9");

        assertThat(doc.apply(target)).isEqualTo("a=9\r\nb=2\r\n");
    }
}
