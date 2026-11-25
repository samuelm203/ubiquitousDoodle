# Orders – Beispiel-Enterprise-App (Java · MVC · Vaadin)

Eine kleine Auftragsverwaltung mit Kunden, Produkten und Bestellungen. Die Bestellungen haben einen Status-Workflow.
Das **Backend ist reines Java nach dem MVC-Muster** und hat keine Framework-Abhängigkeit.
Spring Boot, Vaadin und JPA hängen nur außen dran und lassen sich austauschen.

## Starten

```bash
mvn install                          # baut alles und führt alle Tests aus
mvn -pl app-vaadin spring-boot:run   # http://localhost:8080
PORT=8090 mvn -pl app-vaadin spring-boot:run   # anderer Port
```

| Benutzer | Passwort | Rechte |
|---|---|---|
| `admin` | `admin` | alles, inkl. Produkte pflegen und Kunden löschen |
| `user`  | `user`  | Kunden und Bestellungen |

Beim ersten Start werden Demo-Daten angelegt (`app.demo-data=true`). Die H2-Datenbank liegt unter `app-vaadin/data/`.
Zum Zurücksetzen löschst du diesen Ordner.

Produktions-Build: `mvn -Pproduction package`, danach `java -jar app-vaadin/target/app-vaadin-1.0.0-SNAPSHOT.jar`.

## Architektur

```
┌──────────────── app-vaadin (Spring Boot) ─────────────────┐
│  View: Vaadin (ui/*View)        REST: Spring MVC (rest/*)  │
│            │                              │               │
│            └──────────┬───────────────────┘               │
│   BackendConfig: verdrahtet Repositories + Transaktionen  │
└───────────────────────┼───────────────────────────────────┘
                        ▼
┌──────────────── backend-core (reines Java) ───────────────┐
│  controller/  Eingaben der View → Model, gibt DTOs zurück │
│  service/     Geschäftsabläufe, Transaktionsgrenze        │
│  model/       Entitäten + Regeln (Order, Money, ...)      │
│  repository/  Persistenz-Interfaces                       │
│  Backend      manuelle Verdrahtung ohne DI-Framework      │
└───────────────────────┬───────────────────────────────────┘
                        ▼ implementiert von
   backend-persistence-jpa        backend-persistence-inmemory
   (jakarta.persistence)          (Maps, für Tests/Experimente)
```

**Model:** `model/` enthält die Fachobjekte mit ihren Regeln, zum Beispiel:
- Eine leere Bestellung kann nicht aufgegeben werden.
- Preise werden beim Hinzufügen eingefroren.
- Nur erlaubte Statuswechsel sind möglich: `DRAFT → PLACED → SHIPPED`, Stornieren aus `DRAFT` oder `PLACED`.

**View:** Die Vaadin-Views kennen nur Controller und DTOs, nie Model-Objekte.

**Controller:** `controller/` übersetzt View-Eingaben (Strings, Formulare) in Service-Aufrufe und gibt DTOs zurück.
Dieselben Controller bedienen Vaadin direkt und die REST-API über dünne `@RestController`-Wrapper.

Fehler:
- `DomainException`: fachlicher Fehler, wird zur Notification in der UI bzw. zu HTTP 422 (Problem Details).
- `NotFoundException`: wird zu HTTP 404.

Alternatives Frontend in Vue.js (gegen die REST-API): siehe [`frontend-vue/README.md`](frontend-vue/README.md),
Start mit `cd frontend-vue && npm install && npm run dev` (http://localhost:5173).

Alternatives Frontend in Angular: siehe [`frontend-angular/README.md`](frontend-angular/README.md),
Start mit `cd frontend-angular && npm install && npm start` (http://localhost:4200).

Alternatives Frontend in React (ebenfalls gegen die REST-API): siehe [`frontend-react/README.md`](frontend-react/README.md),
Start mit `cd frontend-react && npm install && npm run dev` (http://localhost:5174).

Desktop-App in JavaFX (nutzt `backend-core` direkt, ohne Spring und REST): siehe [`app-javafx/README.md`](app-javafx/README.md),
Start mit `mvn -f app-javafx/pom.xml javafx:run`.

## REST-API (HTTP Basic)

```bash
curl -u user:user localhost:8080/api/dashboard
curl -u user:user localhost:8080/api/orders
curl -u user:user -H 'Content-Type: application/json' -d '{"customerId":"<id>"}' localhost:8080/api/orders
curl -u user:user -H 'Content-Type: application/json' -d '{"productId":"<id>","quantity":2}' localhost:8080/api/orders/<id>/lines
curl -u user:user -X POST localhost:8080/api/orders/<id>/place     # bzw. /ship, /cancel
```

## Mit einem anderen Framework ausprobieren

Neues Modul anlegen, z. B. `app-quarkus`, `app-micronaut`, `app-javalin` oder `app-helidon`.
Es braucht `backend-core` und einen Persistence-Adapter als Abhängigkeit und stellt dann nur drei Dinge bereit:

1. **Repositories:** entweder `backend-persistence-jpa` mit dem `EntityManager` des Frameworks,
   oder `backend-persistence-inmemory` für den schnellsten Einstieg.
2. **`UnitOfWork`:** eine Implementierung für die Transaktionen des Frameworks.
   Beispiele: `QuarkusTransaction.requiringNew().call(...)`, `TransactionOperations` in Micronaut, oder no-op in-memory.
3. **`new Backend(...)`:** die Controller daraus als Beans/Handler verwenden.

Der kleinste mögliche Start, ohne Datenbank und ohne Framework:

```java
Backend backend = InMemoryBackend.create();
var c = backend.customers.create(new CustomerForm("ACME", "info@acme.ch", "Bern", null));
```

Die Vorlage für Spring ist `app-vaadin/.../config/BackendConfig.java`. Das ist die einzige Klasse, die Spring und das Backend zusammenbringt.

## Tests

- `backend-core`: Tests der Fachregeln (`OrderTest`).
- `backend-persistence-inmemory`: durchgehende Tests Controller → Service → Model, ohne Framework (`BackendTest`).
- `app-vaadin`: REST → JPA → H2 inkl. Security und Flyway-Schema (`OrderApiTest`).

## Bewusst vereinfacht (für Produktion erweitern)

- Benutzer liegen im Speicher und haben `{noop}`-Passwörter. In Produktion: OIDC (Keycloak, Entra ID) oder LDAP.
- Kein Optimistic Locking und keine Pagination (Listen werden komplett geladen).
- H2 statt PostgreSQL. Zum Wechseln Treiber und URL tauschen; die Flyway-Migrationen sind Standard-SQL.
