# AGENTS.md – backend-core

Reines Java, **keine Framework-Abhängigkeit** (kein Spring, kein JPA, kein Jakarta-CDI). Das ist die Kernregel dieses Moduls.
Wer hier ein Framework-Import einführt, bricht die Austauschbarkeit aller Apps.

## Aufbau (`com.example.orders`)

- `model/` – Fachobjekte und Regeln: `Order`, `OrderLine`, `OrderStatus`, `Customer`, `Product`, `Money`, `EmailAddress`, `Sku`,
  typisierte IDs (`OrderId`, ...), `CompanyProperties`. Fehler: `DomainException`.
- `service/` – Geschäftsabläufe und Transaktionsgrenze über `UnitOfWork`. Fehler: `NotFoundException`.
- `controller/` – nimmt Strings/Formulare der UIs, ruft Services, gibt DTOs zurück (`controller/dto/Dtos`, `PropertyDtos`).
- `repository/` – nur Interfaces. Implementierungen liegen in den `backend-persistence-*`-Modulen.
- `Backend` – manuelle Verdrahtung ohne DI. `DemoData` – Beispieldaten. `PropertiesModule` – Verdrahtung der Firmenkonfiguration.

## Regeln

- Fachregeln stehen im Model: leere Bestellung nicht aufgebbar, Preise werden beim Hinzufügen eingefroren,
  erlaubte Statuswechsel `DRAFT -> PLACED -> SHIPPED`, Stornieren aus `DRAFT`/`PLACED`.
- Fachliche Fehler immer als `DomainException` (UI: Meldung, REST: 422) bzw. `NotFoundException` (REST: 404).
- UIs bekommen nur DTOs. Neue Felder zuerst im DTO und Controller anlegen, dann in allen UIs anzeigen.
- Neue Repository-Methoden müssen in **allen** Adaptern (inmemory, jpa, files) implementiert werden.
- Tests: `src/test/.../model/OrderTest` für Fachregeln. Integrationstests liegen in `backend-persistence-inmemory`.

```bash
mvn -pl backend-core test
```
