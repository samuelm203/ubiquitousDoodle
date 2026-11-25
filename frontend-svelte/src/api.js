// Dünner Client für die REST-API. Zugangsdaten (HTTP Basic) liegen nur in der sessionStorage.
import { auth, saveAuth, clearAuth } from './auth.svelte.js'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

async function request(method, path, body, header = auth.header) {
  const res = await fetch('/api' + path, {
    method,
    headers: {
      Accept: 'application/json',
      // Spring antwortet dann mit 401 ohne WWW-Authenticate -> kein Browser-Login-Dialog
      'X-Requested-With': 'XMLHttpRequest',
      ...(header ? { Authorization: header } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  }).catch(() => {
    throw new ApiError(0, 'Server nicht erreichbar')
  })

  if (res.status === 401) {
    if (header === auth.header) onUnauthorized()
    throw new ApiError(401, 'Benutzername oder Passwort falsch')
  }
  if (res.status === 403) throw new ApiError(403, 'Dafür fehlt die Berechtigung (nur Admin)')
  if (!res.ok) {
    let detail = `Fehler ${res.status}`
    try {
      detail = (await res.json()).detail || detail // ProblemDetail
    } catch { /* kein JSON */ }
    throw new ApiError(res.status, detail)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  // HTTP Basic hat keinen Login-Endpoint: ein geschützter GET prüft die Zugangsdaten
  async login(username, password) {
    const header = 'Basic ' + btoa(unescape(encodeURIComponent(`${username}:${password}`)))
    await request('GET', '/dashboard', undefined, header)
    saveAuth(username, header)
  },
  logout: clearAuth,

  dashboard: () => request('GET', '/dashboard'),

  customers: () => request('GET', '/customers'),
  createCustomer: (f) => request('POST', '/customers', f),
  updateCustomer: (id, f) => request('PUT', `/customers/${id}`, f),
  deleteCustomer: (id) => request('DELETE', `/customers/${id}`),

  products: () => request('GET', '/products'),
  createProduct: (f) => request('POST', '/products', f),
  updateProduct: (id, f) => request('PUT', `/products/${id}`, f),

  orders: () => request('GET', '/orders'),
  createOrder: (customerId) => request('POST', '/orders', { customerId }),
  addLine: (id, productId, quantity) => request('POST', `/orders/${id}/lines`, { productId, quantity }),
  removeLine: (id, productId) => request('DELETE', `/orders/${id}/lines/${productId}`),
  place: (id) => request('POST', `/orders/${id}/place`),
  ship: (id) => request('POST', `/orders/${id}/ship`),
  cancel: (id) => request('POST', `/orders/${id}/cancel`),
}
