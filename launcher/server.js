// Launcher: kleine Web-UI zum Starten/Stoppen der Framework-Varianten (keine Abhängigkeiten).
// Start: node launcher/server.js   ->  http://localhost:9000
const http = require('http')
const net = require('net')
const fs = require('fs')
const path = require('path')
const { spawn, execFile } = require('child_process')

const ROOT = path.resolve(__dirname, '..')
const PORT = Number(process.env.LAUNCHER_PORT || 9000)
const WIN = process.platform === 'win32'
const MVN = WIN ? 'mvn.cmd' : 'mvn'
const NPM = WIN ? 'npm.cmd' : 'npm'

// kind: web = Browser-Oberfläche (Port), desktop = eigenes Fenster, backend = REST-API + Vaadin
const SERVICES = [
  { id: 'vaadin', name: 'Vaadin', kind: 'backend', port: 8080, cmd: MVN, args: ['-pl', 'app-vaadin', 'spring-boot:run'], cwd: '.',
    note: 'Java-UI und REST-Backend für Vue, React, Svelte und Angular' },
  { id: 'jsf', name: 'JSF (Quarkus)', kind: 'web', port: 8081, cmd: MVN, args: ['-f', 'app-jsf/pom.xml', 'quarkus:dev', '-Dquarkus.console.enabled=false'], cwd: '.',
    note: 'PrimeFaces, eigenständig mit In-Memory-Daten' },
  { id: 'vue', name: 'Vue', kind: 'web', port: 5173, cmd: NPM, args: ['run', 'dev'], cwd: 'frontend-vue', needs: ['vaadin'] },
  { id: 'react', name: 'React', kind: 'web', port: 5174, cmd: NPM, args: ['run', 'dev'], cwd: 'frontend-react', needs: ['vaadin'] },
  { id: 'svelte', name: 'Svelte', kind: 'web', port: 5175, cmd: NPM, args: ['run', 'dev'], cwd: 'frontend-svelte', needs: ['vaadin'] },
  { id: 'angular', name: 'Angular', kind: 'web', port: 4200, cmd: NPM, args: ['run', 'start'], cwd: 'frontend-angular', needs: ['vaadin'] },
  { id: 'swing', name: 'Swing', kind: 'desktop', cmd: MVN, args: ['-f', 'app-swing/pom.xml', 'exec:java'], cwd: '.',
    note: 'Desktop-Fenster, In-Memory' },
  { id: 'javafx', name: 'JavaFX', kind: 'desktop', cmd: MVN, args: ['-f', 'app-javafx/pom.xml', 'javafx:run'], cwd: '.',
    env: { ORDERS_ROLE: 'ADMIN' }, note: 'Desktop-Fenster, In-Memory, als ADMIN' },
]
const byId = Object.fromEntries(SERVICES.map(s => [s.id, s]))

const state = {} // id -> { proc, log[], phase, portOpen }
for (const s of SERVICES) state[s.id] = { proc: null, log: [], phase: 'stopped', portOpen: false }

function addLog(id, text) {
  const st = state[id]
  for (const line of String(text).replace(/\u001b\[[0-9;]*m/g, '').split(/\r?\n/)) if (line) st.log.push(line)
  if (st.log.length > 500) st.log.splice(0, st.log.length - 500)
}

function probe(port) {
  return new Promise(resolve => {
    const sock = net.connect({ port, host: 'localhost' }) // localhost: Vite bindet teils nur an IPv6
    const done = ok => { sock.destroy(); resolve(ok) }
    sock.setTimeout(500, () => done(false))
    sock.once('connect', () => done(true))
    sock.once('error', () => done(false))
  })
}

async function refresh() {
  await Promise.all(SERVICES.filter(s => s.port).map(async s => { state[s.id].portOpen = await probe(s.port) }))
  for (const s of SERVICES) {
    const st = state[s.id]
    if (st.proc && st.phase === 'starting' && (s.port ? st.portOpen : Date.now() - st.startedAt > 4000)) st.phase = 'running'
  }
}
setInterval(refresh, 1000)

function publicState() {
  return SERVICES.map(s => {
    const st = state[s.id]
    let status = st.phase // stopped | starting | running | stopping
    if (!st.proc && st.portOpen) status = 'external'
    return { id: s.id, name: s.name, kind: s.kind, port: s.port || null, needs: s.needs || [], note: s.note || '',
      status, url: s.port ? `http://localhost:${s.port}/` : null }
  })
}

function start(id) {
  const s = byId[id], st = state[id]
  if (st.proc || st.portOpen) return
  for (const dep of s.needs || []) start(dep)
  st.log = []
  st.phase = 'starting'
  st.startedAt = Date.now()
  addLog(id, `> ${s.cmd} ${s.args.join(' ')}`)
  const proc = spawn(s.cmd, s.args, {
    cwd: path.join(ROOT, s.cwd), shell: WIN, detached: !WIN,
    env: { ...process.env, ...(s.env || {}) }, windowsHide: true,
  })
  st.proc = proc
  proc.stdout.on('data', d => addLog(id, d))
  proc.stderr.on('data', d => addLog(id, d))
  proc.on('error', e => addLog(id, 'Fehler: ' + e.message))
  proc.on('exit', code => {
    addLog(id, `Prozess beendet (Code ${code})`)
    if (st.proc === proc) { st.proc = null; st.phase = 'stopped' }
  })
}

function killTree(proc) {
  if (WIN) execFile('taskkill', ['/PID', String(proc.pid), '/T', '/F'], () => {})
  else try { process.kill(-proc.pid, 'SIGTERM') } catch {}
}

function stop(id) {
  const st = state[id]
  if (!st.proc) return
  st.phase = 'stopping'
  killTree(st.proc)
}

function stopAll() { for (const s of SERVICES) if (state[s.id].proc) killTree(state[s.id].proc) }
process.on('SIGINT', () => { stopAll(); setTimeout(() => process.exit(0), 500) })
process.on('SIGTERM', () => { stopAll(); setTimeout(() => process.exit(0), 500) })
process.on('exit', stopAll)

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://x')
  const json = (code, body) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(body)) }
  if (req.method === 'GET' && url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    return res.end(fs.readFileSync(path.join(__dirname, 'index.html')))
  }
  if (req.method === 'GET' && url.pathname === '/api/state') return json(200, publicState())
  const m = url.pathname.match(/^\/api\/(start|stop|log)\/([a-z]+)$/)
  if (m && byId[m[2]]) {
    if (m[1] === 'log' && req.method === 'GET') return json(200, state[m[2]].log)
    if (req.method === 'POST' && m[1] === 'start') { start(m[2]); return json(200, { ok: true }) }
    if (req.method === 'POST' && m[1] === 'stop') { stop(m[2]); return json(200, { ok: true }) }
  }
  json(404, { error: 'not found' })
})
server.listen(PORT, '127.0.0.1', () => console.log(`Launcher: http://localhost:${PORT}`))
