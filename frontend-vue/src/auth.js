import { reactive } from 'vue'

// HTTP Basic: Zugangsdaten nur für die Browser-Sitzung merken (sessionStorage, nicht localStorage).
const KEY = 'orders.auth'

const saved = JSON.parse(sessionStorage.getItem(KEY) || 'null')

export const auth = reactive({
  username: saved?.username ?? null,
  token: saved?.token ?? null,
  get loggedIn() {
    return this.token !== null
  },
})

export function setCredentials(username, password) {
  auth.username = username
  // btoa kann nur Latin-1; über TextEncoder sind auch Umlaute im Passwort möglich
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  auth.token = btoa(String.fromCharCode(...bytes))
  sessionStorage.setItem(KEY, JSON.stringify({ username: auth.username, token: auth.token }))
}

export function logout() {
  auth.username = null
  auth.token = null
  sessionStorage.removeItem(KEY)
}
