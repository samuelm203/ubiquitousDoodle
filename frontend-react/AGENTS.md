# AGENTS.md – frontend-react

React 19 + Vite + React Router (Port 5174), Single-Page-App gegen die REST-API von `app-vaadin`. Details: `README.md`.
Das Backend muss auf Port 8080 laufen, Ziel änderbar mit `API_TARGET`.

## Aufbau (`src/`)

- `api.js` (fetch-Client, HTTP Basic, Problem-Details-Fehler), `auth.jsx` (Login-Kontext), `hooks.js`, `toast.jsx`, `main.jsx`, `App.jsx`.
- `pages/` – `Dashboard`, `Orders`, `Customers`, `Products`, `Properties`, `Login`. Styles in `styles.css`.

## Regeln

- Kein CORS: Vite leitet `/api` weiter, auch für `npm run preview` (`vite.config.js`). Keine absoluten Backend-URLs.
- Jeder Request sendet `X-Requested-With: XMLHttpRequest` (verhindert den Browser-Login-Dialog bei 401).
- Fehler: 422/404 als Problem Details -> Toast, 403 -> "nur Admin". Zugangsdaten nur im `sessionStorage`.
- Die Properties-Seite nur für ADMIN. Funktionsumfang wie in den anderen UIs halten.
- Echter Betrieb von `dist/` braucht einen Reverse Proxy für `/api`.

```bash
npm install && npm run dev      # http://localhost:5174
npm run build
```
