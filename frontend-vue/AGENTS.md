# AGENTS.md – frontend-vue

Vue 3 + Vite + Vue Router (Port 5173), Single-Page-App gegen die REST-API von `app-vaadin`. Details: `README.md`.
Das Backend muss auf Port 8080 laufen (`mvn -pl app-vaadin spring-boot:run`), Ziel änderbar mit `API_TARGET`.

## Aufbau (`src/`)

- `api.js` (fetch-Client, HTTP Basic, Problem-Details-Fehler), `auth.js` (Login, `sessionStorage`), `router.js`, `notify.js` (Toasts), `format.js`.
- `views/` – `Dashboard`, `Orders`, `Customers`, `Products`, `Properties`, `Login`. `components/` – `ModalDialog`, `StatusBadge`, `ToastStack`.

## Regeln

- Kein CORS: Vite leitet `/api` weiter (`vite.config.js`). Nie absolute Backend-URLs im Code.
- Jeder Request sendet `X-Requested-With: XMLHttpRequest`, sonst zeigt der Browser bei 401 einen Login-Dialog.
- Fehler: 422/404 als Problem Details (`detail` als Toast), 403 als Hinweis "nur Admin". Zugangsdaten nur im `sessionStorage`.
- Die Properties-Ansicht nur für ADMIN. Funktionsumfang muss mit React/Svelte/Angular/Vaadin übereinstimmen.
- Dieser Ordner hat uncommittete Änderungen (`vite.config.js`, PWA, `public/`): vor größeren Eingriffen `git status` prüfen.

```bash
npm install && npm run dev      # http://localhost:5173
npm run build
```
