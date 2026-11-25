# AGENTS.md – launcher

Kleine Web-UI (Port 9000, `LAUNCHER_PORT`), um die Framework-Varianten per Klick zu starten, zu stoppen und nebeneinander zu vergleichen.
**Keine npm-Abhängigkeiten**, nur Node-Standardbibliothek. Das soll so bleiben.

## Dateien

- `server.js` – HTTP-Server und Prozessverwaltung. Die Liste `SERVICES` oben ist die einzige Konfiguration (id, Port, Befehl, Arbeitsordner, `needs`).
  API: `GET /api/state`, `GET /api/log/<id>`, `POST /api/start/<id>`, `POST /api/stop/<id>`.
- `index.html` – komplette UI (Kacheln, Log, Vergleichsansicht mit iframes), ohne Build-Schritt.

## Regeln

- Neue Variante = Eintrag in `SERVICES` (Port aus der jeweiligen Konfiguration übernehmen). Web-Frontends tragen `needs: ['vaadin']`.
- Status kommt aus dem Prozess **und** einem Port-Check auf `localhost` (nicht `127.0.0.1`: Vite bindet teils nur an IPv6).
  Läuft ein Port ohne unseren Prozess, ist der Status `external`.
- Beenden: unter Windows `taskkill /T /F` auf den Prozessbaum, sonst Prozessgruppe. Beim Beenden des Launchers werden alle gestarteten Prozesse mitgestoppt.
- Der Server lauscht nur auf `127.0.0.1`, denn er startet beliebige Befehle. Nie auf `0.0.0.0` öffnen und keine frei übergebenen Befehle zulassen
  (die API nimmt nur IDs aus `SERVICES`).
- Voraussetzung: einmalig `mvn install` im Root und `npm install` in den Frontends.

```bash
node launcher/server.js         # http://localhost:9000
```
