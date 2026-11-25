# frontend-react

React-Oberfläche (Vite, React 19, React Router) für die REST-API aus `app-vaadin`. Sie bietet dieselben Funktionen wie die Vaadin-UI:
Dashboard, Kunden, Produkte, Bestellungen mit Positionen und Status-Workflow.

## Starten

```bash
# 1. Backend starten (im Repo-Root)
mvn -pl app-vaadin spring-boot:run            # Port 8080

# 2. Frontend starten
cd frontend-react
npm install
npm run dev                                   # http://localhost:5174
```

Läuft das Backend auf einem anderen Port: `API_TARGET=http://localhost:8090 npm run dev`.
Der Vite-Dev-Server leitet `/api` ans Backend weiter, deshalb braucht das Backend kein CORS.

Anmeldung: `admin` / `admin` (darf auch Produkte ändern) oder `user` / `user`.

`npm run build` erzeugt statische Dateien in `dist/`. Die Datei `vite.config.js` enthält den Proxy auch für `npm run preview`.
Für echten Betrieb muss ein Reverse Proxy `/api` an das Backend weiterreichen.

## Aufbau

| Datei | Zweck |
|---|---|
| `src/api.js` | `fetch`-Client: HTTP Basic, Fehler aus Problem Details, Formatierung |
| `src/auth.jsx` | Login-Zustand (sessionStorage), automatischer Logout bei 401 |
| `src/toast.jsx`, `src/hooks.js` | Meldungen und Lade-Hook |
| `src/pages/*` | Dashboard, Kunden, Produkte, Bestellungen, Login |

Der Client sendet `X-Requested-With: XMLHttpRequest`. Dadurch antwortet Spring bei 401 ohne `WWW-Authenticate`,
und der Browser zeigt keinen eigenen Login-Dialog.

## Bekannte Grenzen

- Die API hat keinen "Wer bin ich"-Endpunkt. Die Produktseite ist deshalb für alle sichtbar; Nicht-Admins bekommen beim Speichern eine 403-Meldung.
- Das Passwort liegt (als Basic-Header) in der sessionStorage. Für Produktion: OIDC mit Token statt Basic.
- Keine automatisierten Frontend-Tests.
