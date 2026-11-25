# AGENTS.md – backend-persistence-jpa

JPA-Adapter (`jakarta.persistence`) für `backend-core`. Wird von `app-vaadin` mit Hibernate/H2 genutzt.

## Inhalt

- `entity/` – `CustomerEntity`, `OrderEntity`, `OrderLineEmbeddable`, `ProductEntity`. Das sind reine Persistenz-Typen, nicht die Fachobjekte.
- `Jpa{Customer,Order,Product}Repository` – setzen die Interfaces aus `backend-core/repository` um und mappen Entity <-> Model.

## Regeln

- Das Model aus `backend-core` bekommt **keine** JPA-Annotationen. Mapping passiert nur hier.
- Der `EntityManager` und die Transaktion (`UnitOfWork`) kommen vom Framework der App (siehe `app-vaadin/.../config/BackendConfig`).
- Schemaänderungen: Entity ändern **und** Flyway-Migration in `app-vaadin/src/main/resources/db/migration` ergänzen
  (neue Datei `V<n>__...sql`, bestehende Migrationen nie ändern).
- Die Firmenkonfiguration (`Properties`) liegt nicht in der Datenbank, sondern in `backend-persistence-files`.
- Tests laufen über `app-vaadin` (`OrderApiTest`: REST -> JPA -> H2).
