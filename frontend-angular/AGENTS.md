# AGENTS.md – frontend-angular

Angular 21 mit Standalone-Komponenten, Signals und zoneless (Port 4200). Single-Page-App gegen die REST-API von `app-vaadin`. Details: `README.md`.
Das Backend muss auf Port 8080 laufen, Ziel änderbar mit `API_TARGET` (`proxy.conf.js`).

## Aufbau (`src/app/`)

- `api.ts` (HttpInterceptor: `Authorization`, `X-Requested-With`, 401 -> Abmelden, Fehler -> `ApiError`), `auth.ts`, `notify.ts`, `format.ts`, `title.ts`.
- `app.config.ts`, `app.routes.ts`, `app.ts`, `shell.ts` (Layout).
- `views/` – `dashboard`, `orders`, `customers`, `products`, `properties`, `login`. `components/` – `modal-dialog`, `status-badge`, `toast-stack`.

## Regeln

- Standalone-Komponenten und Signals, keine NgModules, keine Zone.js-Abhängigkeit einführen.
- Kein CORS: der Dev-Server leitet `/api` weiter. Keine absoluten Backend-URLs.
- Fehler: 422/404 als Problem Details -> Toast, 403 -> "nur Admin". Zugangsdaten nur im `sessionStorage`.
- Properties-Ansicht nur für ADMIN. Funktionsumfang wie bei den anderen UIs.
- `dist/` ist Build-Ausgabe, nicht bearbeiten.

```bash
npm install && npm start        # http://localhost:4200
npm run build
```
