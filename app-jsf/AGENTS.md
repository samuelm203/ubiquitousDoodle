# AGENTS.md – app-jsf

Quarkus mit JSF/PrimeFaces (Port 8081, `PORT` überschreibt). Eigenständig: nutzt `backend-core` direkt mit `InMemoryBackend`
und `backend-persistence-files`, **ohne** REST und ohne die Vaadin-App. Demo-Daten bei jedem Start frisch (`app.demo-data=true`).

## Aufbau

- `src/main/java/.../jsf/` – CDI-Beans: `CustomersBean`, `ProductsBean`, `OrdersBean`, `DashboardBean`, `PropertiesBean`, `RoleBean`.
  Hilfsklassen `Fmt`, `Messages`. `BackendProducer` und `PropertiesProducer` sind die einzigen Stellen, die CDI und Backend verbinden.
- `src/main/resources/META-INF/resources/*.xhtml` – Seiten (`template.xhtml` als Layout), `app.css`, `index.html`.
- `application.properties` – Port, Demo-Daten, `app.properties.root=./data/properties`.

## Regeln

- Beans kennen nur Controller und DTOs aus `backend-core`. `Backend` ist `final` mit öffentlichen Feldern, deshalb `@Singleton` ohne Client-Proxy.
- Fachliche Fehler (`DomainException`) werden über `Messages` als Faces-Meldung angezeigt, nicht als Stacktrace.
- Die Konfigurations-Seite (`properties.xhtml`) nur für ADMIN anzeigen, Rolle kommt aus `RoleBean`.
- Funktionen müssen mit den anderen UIs gleichziehen (siehe Root-`AGENTS.md`).

```bash
mvn -f app-jsf/pom.xml quarkus:dev     # Dev-Modus mit Live-Reload
```
