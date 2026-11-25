# app-javafx

Desktop-Oberfläche in **JavaFX 27**. Sie nutzt das framework-freie `backend-core` direkt im selben Prozess.
Es gibt weder Spring noch REST, und die Daten liegen im Speicher.
Das zeigt, dass dieselben Controller auch ohne Webserver funktionieren.

Funktionen wie in der Vaadin-UI: Dashboard, Kunden (suchen, anlegen, bearbeiten, löschen), Produkte,
Bestellungen mit Positionen und Status-Workflow (aufgeben, versenden, stornieren).

Zusätzlich gibt es für **ADMIN** den Tab *Konfiguration* (Firmenkonfiguration als `.properties`-Tabelle: Firma wählen,
filtern, nur Unterschiede, Paging, Zellen bearbeiten oder löschen, gesammelt speichern). Die App hat keinen Login:
die Rolle kommt aus `-Dorders.role=ADMIN` oder der Umgebungsvariable `ORDERS_ROLE=ADMIN` (Standard: USER, Tab fehlt).
Die Dateien liegen unter `-Dorders.properties.root` (Standard `./data/properties`, Beispieldaten beim ersten Start).

## Starten

```bash
mvn -q install                          # einmalig im Repo-Root: baut backend-core usw.
mvn -f app-javafx/pom.xml javafx:run
ORDERS_ROLE=ADMIN mvn -f app-javafx/pom.xml javafx:run   # mit Konfigurations-Tab
```

Beim Start werden Demo-Daten angelegt (`DemoData.seed(backend)`). Beim Beenden sind alle Änderungen weg.

## Aufbau

| Klasse | Aufgabe |
|---|---|
| `OrdersApp` | Einstieg. `InMemoryBackend.create()` + `DemoData.seed(...)`, baut die Tabs |
| `PropertiesTab` | Firmenkonfiguration (nur ADMIN), nutzt `PropertiesController` und `FilePropertiesRepository` |
| `DashboardTab`, `CustomersTab`, `ProductsTab`, `OrdersTab` | Views, kennen nur die Controller und DTOs |
| `Ui` | Hilfsfunktionen: Tabellenspalten, Formatierung, Fehlerdialog bei `DomainException` |

`OrdersApp` ist das einzige Stück, das das Backend verdrahtet. Für Persistenz in einer Datenbank würde man hier
`backend-persistence-jpa` mit einem `EntityManager` und einer eigenen `UnitOfWork` statt `InMemoryBackend` nehmen.

## Hinweise

- Für automatische Smoke-Tests schließt `-Dorders.exitAfterMs=4000` das Fenster nach 4 Sekunden von selbst.
- Es gibt keine automatisierten UI-Tests. Das Backend selbst ist in `backend-persistence-inmemory` getestet.
