# AGENTS.md – app-javafx

JavaFX-Desktop-App (JavaFX 27). Nutzt `backend-core` direkt im selben Prozess, ohne Spring und REST, Daten im Speicher.
Details: `README.md`.

## Aufbau (`com.example.orders.javafx`)

- `OrdersApp` – Einstieg und **einziges** Verdrahten des Backends (`InMemoryBackend.create()` + `DemoData.seed(...)`), baut die Tabs.
- `DashboardTab`, `CustomersTab`, `ProductsTab`, `OrdersTab`, `PropertiesTab` – Views, kennen nur Controller und DTOs.
- `Ui` – Tabellenspalten, Formatierung, Fehlerdialog bei `DomainException`. `Refreshable` – Tabs neu laden.

## Regeln

- Rolle ohne Login: `-Dorders.role=ADMIN` oder Umgebungsvariable `ORDERS_ROLE=ADMIN` (Standard `USER`, dann fehlt der Konfigurations-Tab).
- Properties-Ordner: `-Dorders.properties.root` (Standard `./data/properties`).
- UI-Änderungen auf dem JavaFX Application Thread. Backend-Aufrufe sind schnell (In-Memory), deshalb ohne Extra-Threads.
- Für Smoke-Tests `-Dorders.exitAfterMs=4000`. Es gibt keine automatisierten UI-Tests.

```bash
mvn -q install                                           # einmalig im Root
mvn -f app-javafx/pom.xml javafx:run
ORDERS_ROLE=ADMIN mvn -f app-javafx/pom.xml javafx:run
```
