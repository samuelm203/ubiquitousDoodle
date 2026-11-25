// Login-Zustand als reaktives Objekt (Svelte 5 Runes). Nur für die Browser-Sitzung gespeichert.
const KEY = 'orders.auth'

function load() {
  try {
    return JSON.parse(sessionStorage.getItem(KEY)) ?? {}
  } catch {
    return {}
  }
}

const saved = load()

export const auth = $state({
  username: saved.username ?? null,
  header: saved.header ?? null,
})

export function saveAuth(username, header) {
  auth.username = username
  auth.header = header
  sessionStorage.setItem(KEY, JSON.stringify({ username, header }))
}

export function clearAuth() {
  auth.username = null
  auth.header = null
  sessionStorage.removeItem(KEY)
}
