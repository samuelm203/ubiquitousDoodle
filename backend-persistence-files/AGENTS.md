# AGENTS.md – backend-persistence-files

Dateibasierte Persistenz der Firmenkonfiguration (`.properties`-Tabelle). Nur ADMIN darf sie lesen oder schreiben.

## Inhalt

- `FilePropertiesRepository` – liest und schreibt `<root>/<firma>/<spalte>.properties`. Standard-Root `./data/properties`
  (Einstellung `app.properties.root` bzw. `-Dorders.properties.root`).
- `PropertiesDocument` – Parser/Writer einer einzelnen Datei. Erhält **Kommentare, Leerzeilen und Reihenfolge**.
- `DemoProperties` – erzeugt beim ersten Start 5 Firmen mit je 4 Dateien (dev/prod/staging/test, je ca. 1000 Schlüssel).

## Regeln

- Geschrieben wird über eine temporäre Datei, dann ersetzen (nie direkt in die Zieldatei).
- Mehrere Änderungen sind alles-oder-nichts. `value = null` löscht den Schlüssel, ein neuer Schlüssel wird angehängt.
- Keine Versionsprüfung: gleichzeitige Handänderungen können überschrieben werden. Das ist bewusst vereinfacht.
- Schlüssel und Werte nie loggen, sie können Geheimnisse enthalten.
- Tests: `PropertiesDocumentTest` (Parser/Writer, Round-Trip), `FilePropertiesRepositoryTest`. Bei Parser-Änderungen immer den Round-Trip testen.

```bash
mvn -pl backend-persistence-files test
```
