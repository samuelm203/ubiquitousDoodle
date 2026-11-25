# app-swing

Swing-Desktop-Oberfläche für die Beispiel-Auftragsverwaltung, gebaut mit der deklarativen Swing-DSL
[Swing-Tree](https://github.com/globaltcad/swing-tree) (`io.github.globaltcad:swing-tree:1.1.2`) und FlatLaf (3.8) als Look and Feel.

## Architektur

Die App läuft in **einem Prozess** und braucht weder Spring noch REST:

```
Swing-Panels  ->  Controller (backend-core)  ->  Services  ->  Model / Repositories (backend-persistence-inmemory)
```

- Abhängigkeiten: `backend-core` und `backend-persistence-inmemory`.
- `SwingApp` erzeugt `InMemoryBackend.create()`, füllt es mit `DemoData.seed(backend)` und übergibt `backend` an das Hauptfenster.
- Die Panels verwenden **nur** `backend.customers`, `.products`, `.orders`, `.dashboard` und die DTOs aus `Dtos`. Model-Objekte und Services sieht die UI nie.
- Fachliche Fehler (`DomainException`, `NotFoundException`) werden in `UiSupport.call(...)` abgefangen und als Dialog angezeigt.
- Alles läuft im Event-Dispatch-Thread (Backend-Aufrufe sind In-Memory und schnell). Swing-Tree wird auf `EventProcessor.COUPLED` gestellt, damit seine Event-Handler direkt im EDT laufen.

## Funktionsumfang

Wie die Vaadin-UI: Dashboard, Kunden (suchen, anlegen, bearbeiten, löschen), Produkte (anlegen, bearbeiten),
Bestellungen (anlegen, Positionen hinzufügen/entfernen, aufgeben, versenden, stornieren). Beim Tabwechsel wird das Panel neu geladen.

Der Löschen-Button bei Kunden fragt vorher nach. Es gibt keine Anmeldung; die Rolle kommt aus `-Dorders.role=ADMIN` (Standard USER).

**Konfiguration (nur ADMIN):** Mit `-Dorders.role=ADMIN` erscheint der Tab «Konfiguration» (Firmenauswahl, Filter, «Nur Unterschiede», Paging mit 100 Zeilen, Zellen bearbeiten, «Löschen (null)», gesammeltes «Speichern» alles-oder-nichts). Dateien unter `-Dapp.properties.root` (Standard `./data/properties`, Demo-Daten beim ersten Start). Start: `mvn -pl app-swing exec:java -Dorders.role=ADMIN`.

## Starten

Aus dem Repo-Root (Java 21+, die Backend-Module müssen einmal installiert sein):

```
mvn -q install -DskipTests
mvn -pl app-swing exec:java
```

Oder als ausführbares Jar (Fat-Jar mit Classifier `all`):

```
mvn -pl app-swing -am package
java -jar app-swing/target/app-swing-1.0.0-SNAPSHOT-all.jar
```

## Tests

`SwingPanelsTest` erzeugt die Panels ohne sichtbares Fenster (Headless) und steuert sie über ihre Bedienelemente an
(Kunden/Produkte anlegen, Bestellung bis Versand, Fehlerdialoge). Der Test des Hauptfensters wird übersprungen, wenn kein Display vorhanden ist.

## Hinweise

- Swing-Tree und sein Zubehör (sprouts, miglayout, jsvg, jspecify, slf4j-api) werden im `pom.xml` fest gepinnt, weil Swing-Tree sie als offene Versionsbereiche deklariert.
- Die Daten liegen im Speicher und sind nach dem Beenden weg.
