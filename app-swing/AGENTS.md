# AGENTS.md – app-swing

Swing-Desktop-App mit der DSL **Swing-Tree** und FlatLaf. Läuft in einem Prozess, ohne Spring und REST, mit `backend-core` +
`backend-persistence-inmemory`. Details und Funktionsumfang: `README.md`.

## Aufbau (`com.example.orders.swing`)

- `SwingApp` – Einstieg: `InMemoryBackend.create()`, `DemoData.seed(...)`, Hauptfenster.
- `MainFrame` – Tabs. Panels: `DashboardPanel`, `CustomersPanel`, `ProductsPanel`, `OrdersPanel`, `PropertiesPanel`.
- `ListTableModel` (generisches Tabellenmodell), `Refreshable` (Panels neu laden), `UiSupport.call(...)` (Fehlerdialog für `DomainException`/`NotFoundException`).

## Regeln

- Panels nutzen **nur** `backend.customers/.products/.orders/.dashboard` und DTOs. Nie Model oder Services.
- Alles im Event-Dispatch-Thread. Swing-Tree steht auf `EventProcessor.COUPLED`, damit Handler direkt im EDT laufen. Keine Hintergrund-Threads einführen, ohne zurück in den EDT zu wechseln.
- Backend-Aufrufe immer in `UiSupport.call(...)` kapseln, damit fachliche Fehler als Dialog erscheinen.
- Der Tab für die Firmenkonfiguration nur für ADMIN.
- Tests: `SwingPanelsTest`, `PropertiesPanelTest` (UI headless prüfen).

```bash
mvn -f app-swing/pom.xml exec:java
mvn -pl app-swing test
```
