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
  admin: saved.admin ?? false,
})

export function saveAuth(username, header, admin = false) {
  auth.username = username
  auth.header = header
  auth.admin = admin
  sessionStorage.setItem(KEY, JSON.stringify({ username, header, admin }))
}

export function clearAuth() {
  auth.username = null
  auth.header = null
  auth.admin = false
  sessionStorage.removeItem(KEY)
}
