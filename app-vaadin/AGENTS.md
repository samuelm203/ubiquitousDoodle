# AGENTS.md – app-vaadin

Spring Boot App (Port 8080): **Vaadin-UI und REST-API** in einem. Die REST-API ist das Backend für `frontend-vue`, `-react`, `-svelte` und `-angular`.
Persistenz: `backend-persistence-jpa` mit H2 (Datei unter `app-vaadin/data/`, Löschen setzt zurück) und Flyway.

## Aufbau (`com.example.orders.app`)

- `config/BackendConfig` – **einzige** Klasse, die Spring und `backend-core` verbindet (Vorlage für andere Frameworks). Weitere: `SecurityConfig`, `PropertiesConfig`, `DemoDataRunner`, `PropertiesDemoRunner`.
- `rest/` – dünne `@RestController` über den Controllern aus `backend-core`. `RestErrorHandler` mappt `DomainException` -> 422, `NotFoundException` -> 404 (Problem Details).
- `ui/` – Vaadin-Views (`MainLayout`, `*View`). Sie kennen nur Controller und DTOs.
- `src/main/resources/db/migration/` – Flyway, Standard-SQL.

## Regeln

- Keine Fachlogik in REST oder Views, sondern in `backend-core`.
- REST-Änderungen sind **Vertragsänderungen** für vier Frontends: Pfade, Feldnamen und Fehlerformat nur abgestimmt ändern und die Frontends nachziehen.
- Sicherheit: HTTP Basic, Rollen `USER`/`ADMIN`, Benutzer im Speicher (`{noop}`, nur Demo). `/api/properties/**` ist ADMIN-only, schon lesend.
  Requests mit `X-Requested-With: XMLHttpRequest` bekommen bei 401 keinen `WWW-Authenticate`-Header (sonst Browser-Login-Dialog).
- Schemaänderung = neue Migration `V<n>__*.sql`, nie eine bestehende ändern.
- `src/main/frontend/generated/` wird von Vaadin erzeugt: nicht bearbeiten, nicht einchecken.
- Port per `PORT=8090`.

```bash
mvn -pl app-vaadin spring-boot:run
mvn -pl app-vaadin test      # OrderApiTest, PropertiesApiTest (REST -> JPA -> H2 inkl. Security)
```
