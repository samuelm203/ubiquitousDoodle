# AGENTS.md (Repo-Root)

Beispiel-Auftragsverwaltung (Kunden, Produkte, Bestellungen, Firmenkonfiguration als `.properties`), mehrfach umgesetzt mit
verschiedenen UI-Frameworks. Ein reines Java-Backend (`backend-core`) wird von allen Varianten genutzt. Details: `README.md`.

## Module

| Ordner | Rolle |
|---|---|
| `backend-core` | Framework-freies MVC-Backend (model, service, controller, DTOs, Repository-Interfaces) |
| `backend-persistence-inmemory` / `-jpa` / `-files` | Persistenz-Adapter für `backend-core` |
| `app-vaadin` | Spring Boot: Vaadin-UI **und** REST-API (`/api/**`, Port 8080) |
| `app-jsf` | Quarkus + PrimeFaces, In-Memory (Port 8081) |
| `app-swing`, `app-javafx` | Desktop-Apps, In-Memory, ohne Webserver |
| `frontend-vue` / `-react` / `-svelte` / `-angular` | SPAs gegen die REST-API von `app-vaadin` (Ports 5173 / 5174 / 5175 / 4200) |
| `launcher` | Web-UI zum Starten und Vergleichen der Varianten (`node launcher/server.js`, Port 9000) |
| `data` | Laufzeitdaten (H2-Datei, Properties-Beispieldateien), nicht von Hand ändern |

## Regeln, die überall gelten

- Sprache der Doku, Kommentare und UI-Texte ist Deutsch. Code-Bezeichner sind Englisch.
- Abhängigkeitsrichtung: Apps und Frontends -> Controller/DTOs -> Services -> Model. Views sehen nie Model-Objekte oder Services.
- Fachliche Regeln gehören ins `model/` von `backend-core`, nie in eine UI. Jedes Frontend muss dieselben Funktionen haben
  (Dashboard, Kunden, Produkte, Bestellungen mit Status-Workflow, Firmenkonfiguration nur für ADMIN).
- Neue Funktion = in `backend-core` anlegen, dann in **allen** UIs nachziehen, sonst driften die Varianten auseinander.
- Demo-Logins: `admin/admin` (alles) und `user/user` (ohne Produkte ändern, ohne Konfiguration).
- Keine Geheimnisse einchecken. Der Konfigurations-Bereich ist ADMIN-only, weil Werte Geheimnisse enthalten können.

## Bauen und prüfen

```bash
mvn install                 # alles bauen, alle Tests (Root)
mvn -pl app-vaadin spring-boot:run
cd frontend-<name> && npm install && npm run dev
```

Es gibt keine automatisierten UI-Tests der Frontends. Änderungen daran im Browser prüfen (Backend muss laufen).
Generierte Ordner nicht bearbeiten oder einchecken: `target/`, `dist/`, `node_modules/`, `app-vaadin/src/main/frontend/generated/`.
