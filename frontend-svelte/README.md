# Orders – Svelte-Frontend

Single-Page-App (Svelte 5 + Vite) gegen die REST-API aus `app-vaadin`. Gleicher Umfang wie `frontend-vue` und `frontend-react`:
Login, Übersicht, Bestellungen mit Positionen und Status-Workflow, Kunden und Produkte.

## Starten

```bash
# 1. Backend (im Repo-Root)
mvn -pl app-vaadin spring-boot:run          # http://localhost:8080

# 2. Frontend
cd frontend-svelte
npm install
npm run dev                                 # http://localhost:5175
API_TARGET=http://localhost:8090 npm run dev   # falls das Backend auf einem anderen Port läuft
```

Login: `admin / admin` (alles) oder `user / user` (ohne Produkte ändern).

## Wie es angebunden ist

- **Kein CORS nötig:** Der Vite-Dev-Server leitet `/api/**` an das Backend weiter (`vite.config.js`).
- **HTTP Basic:** Die Zugangsdaten liegen nur in der `sessionStorage`. Beim Login prüft ein `GET /api/dashboard` sie.
- **Kein Browser-Login-Dialog:** Jeder Request schickt `X-Requested-With: XMLHttpRequest`, dann antwortet Spring bei 401
  ohne `WWW-Authenticate`-Header.
- **Fehler:** 422/404 kommen als Problem Details, `detail` erscheint als Toast. 403 wird als „nur Admin“ gemeldet.

## Struktur

```
src/
  api.js              fetch-Client + alle Endpunkte
  auth.svelte.js      Login-Zustand (Runes, $state)
  router.svelte.js    kleiner Hash-Router (#/orders/<id>), keine Zusatzbibliothek
  toast.svelte.js     Toasts, attempt() für API-Aufrufe
  format.js           CHF/Datum-Formatierung, Status-Labels
  views/              Login, Dashboard, Orders, Customers, Products
  components/         StatusBadge, Modal
```

Produktions-Build: `npm run build` → `dist/`. Im Betrieb muss `/api` per Reverse-Proxy auf dasselbe Origin zeigen.
