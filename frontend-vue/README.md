# Orders – Vue.js-Frontend

Single-Page-App (Vue 3 + Vite + Vue Router) gegen die REST-API aus `app-vaadin`. Sie bietet dasselbe wie die Vaadin-Views:
Übersicht, Bestellungen mit Positionen und Status-Workflow, Kunden und Produkte.

## Starten

```bash
# 1. Backend (im Repo-Root)
mvn -pl app-vaadin spring-boot:run          # http://localhost:8080

# 2. Frontend
cd frontend-vue
npm install
npm run dev                                 # http://localhost:5173
API_TARGET=http://localhost:8090 npm run dev   # falls das Backend auf einem anderen Port läuft
```

Login: `admin / admin` (alles) oder `user / user` (ohne Produkte ändern).

## Wie es angebunden ist

- **Kein CORS nötig:** Der Vite-Dev-Server leitet `/api/**` an das Backend weiter (`vite.config.js`).
- **HTTP Basic:** Die Zugangsdaten liegen nur im `sessionStorage` und gehen mit jedem Request mit.
  Ob sie stimmen, prüft beim Login ein `GET /api/dashboard`.
- **Kein Browser-Login-Dialog:** Jeder Request schickt `X-Requested-With: XMLHttpRequest`. Dann antwortet Spring Security
  bei 401 ohne `WWW-Authenticate`-Header.
- **Fehler:** 422/404 kommen als Problem Details. `detail` wird als Toast angezeigt, 403 als Hinweis „nur Admin“.

## Struktur

```
src/
  api.js        fetch-Wrapper + alle Endpunkte
  auth.js       Login-Zustand (Basic-Token)
  router.js     Routen + Login-Guard
  notify.js     Toasts, attempt() für API-Aufrufe
  format.js     CHF/Datum-Formatierung, Status-Labels
  views/        Login, Dashboard, Orders, Customers, Products
  components/   StatusBadge, ModalDialog, ToastStack
```

Produktions-Build: `npm run build` → `dist/`. Für den Betrieb muss `/api` per Reverse-Proxy auf dasselbe Origin zeigen.
Alternativ kann das Backend `dist/` als statische Ressourcen ausliefern.
