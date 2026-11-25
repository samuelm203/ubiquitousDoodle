# AGENTS.md – frontend-svelte

Svelte 5 (Runes) + Vite (Port 5175), Single-Page-App gegen die REST-API von `app-vaadin`. Details: `README.md`.
Das Backend muss auf Port 8080 laufen, Ziel änderbar mit `API_TARGET`.

## Aufbau (`src/`)

- `api.js` (fetch-Client), `auth.svelte.js`, `router.svelte.js` (eigener kleiner Router), `toast.svelte.js`, `format.js`.
- `views/` – `Dashboard`, `Orders`, `Customers`, `Products`, `Properties`, `Login`. `components/` – `Modal`, `StatusBadge`.

## Regeln

- Svelte-5-Syntax verwenden (Runes wie `$state`, `$derived`), keine alten `$:`-Stores. Dateien mit Runes außerhalb von Komponenten heißen `*.svelte.js`.
- Kein CORS: Vite leitet `/api` weiter (`vite.config.js`). Keine absoluten Backend-URLs.
- Jeder Request sendet `X-Requested-With: XMLHttpRequest`. Fehler: 422/404 als Problem Details -> Toast, 403 -> "nur Admin".
- Zugangsdaten nur im `sessionStorage`. Properties-Ansicht nur für ADMIN. Funktionsumfang wie bei den anderen UIs.

```bash
npm install && npm run dev      # http://localhost:5175
npm run build
```
