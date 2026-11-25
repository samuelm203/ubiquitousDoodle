import { reactive } from 'vue'

// HTTP Basic: Zugangsdaten nur für die Browser-Sitzung merken (sessionStorage, nicht localStorage).
const KEY = 'orders.auth'

const saved = JSON.parse(sessionStorage.getItem(KEY) || 'null')

export const auth = reactive({
  username: saved?.username ?? null,
  token: saved?.token ?? null,
  // ADMIN? Das Backend hat keinen Rollen-Endpoint; LoginView prüft per Probe-GET auf /properties/companies.
  admin: saved?.admin ?? false,
  get loggedIn() {
    return this.token !== null
  },
})

export function setCredentials(username, password) {
  auth.username = username
  auth.admin = false
  // btoa kann nur Latin-1; über TextEncoder sind auch Umlaute im Passwort möglich
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  auth.token = btoa(String.fromCharCode(...bytes))
  sessionStorage.setItem(KEY, JSON.stringify({ username: auth.username, token: auth.token, admin: auth.admin }))
}

export function setAdmin(admin) {
  auth.admin = admin
  sessionStorage.setItem(KEY, JSON.stringify({ username: auth.username, token: auth.token, admin }))
}

export function logout() {
  auth.admin = false
  auth.username = null
  auth.token = null
  sessionStorage.removeItem(KEY)
}
