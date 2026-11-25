# Orders – Angular-Frontend

Single-Page-App (Angular 21, Standalone-Komponenten, Signals, zoneless) gegen die REST-API aus `app-vaadin`.
Gleicher Funktionsumfang und gleiches Design wie `frontend-vue`: Übersicht, Bestellungen mit Positionen und Status-Workflow, Kunden und Produkte.

## Starten

```bash
# 1. Backend (im Repo-Root)
mvn -pl app-vaadin spring-boot:run          # http://localhost:8080

# 2. Frontend
cd frontend-angular
npm install
npm start                                   # http://localhost:4200
API_TARGET=http://localhost:8090 npm start  # falls das Backend auf einem anderen Port läuft
```

Login: `admin / admin` (alles) oder `user / user` (ohne Produkte ändern).

## Wie es angebunden ist

- **Kein CORS nötig:** Der Dev-Server leitet `/api/**` an das Backend weiter (`proxy.conf.js`).
- **HTTP Basic:** Ein `HttpInterceptor` (`api.ts`) setzt `Authorization` und `X-Requested-With`, damit kein Browser-Login-Dialog erscheint.
  Bei 401 wird abgemeldet und zum Login geleitet. Fehler (422/404 als Problem Details, 403) werden zu `ApiError` und als Toast angezeigt.
- Die Zugangsdaten liegen nur im `sessionStorage`.

## Struktur

```
src/app/
  api.ts          Interceptor + Api-Service (alle Endpunkte)
  auth.ts         Login-Zustand (Signals)
  app.routes.ts   Routen + Guards (lazy geladen)
  notify.ts       Toasts, attempt() für API-Aufrufe
  format.ts       CHF/Datum-Formatierung, Status-Labels
  shell.ts        Sidebar-Layout
  views/          login, dashboard, orders, customers, products
  components/     status-badge, modal-dialog, toast-stack
```

Produktions-Build: `npm run build` → `dist/browser/`. Für den Betrieb muss `/api` per Reverse-Proxy auf dasselbe Origin zeigen.
