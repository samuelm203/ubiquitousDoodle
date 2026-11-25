# AGENTS.md – backend-persistence-inmemory

Persistenz im Speicher (Maps) für `backend-core`. Für Tests, Experimente und die Apps ohne Datenbank (`app-swing`, `app-javafx`, `app-jsf`).
Beim Beenden sind alle Daten weg.

## Inhalt

- `InMemoryBackend.create()` – baut ein fertig verdrahtetes `Backend` (Repositories, no-op-`UnitOfWork`). Einstiegspunkt der Apps.
- `InMemory{Customer,Order,Product,Properties}Repository` – Implementierungen der Interfaces aus `backend-core/repository`.
- Tests: `BackendTest` (Controller -> Service -> Model ohne Framework), `PropertiesServiceTest`.

## Regeln

- Nur `backend-core` als Abhängigkeit, kein Framework.
- Verhalten muss dem der anderen Adapter entsprechen (z. B. Sortierung, Fehler bei unbekannter ID). Neue Repository-Methoden hier **und** in `-jpa` und `-files` ergänzen.
- `BackendTest` ist der schnellste Weg, Fachlogik end-to-end zu prüfen: neue Abläufe dort testen.

```bash
mvn -pl backend-persistence-inmemory test
```
