// Dünner Client für die REST-API. Zugangsdaten (HTTP Basic) liegen nur in der sessionStorage.

const KEY = 'orders.auth'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

export function loadAuth() {
  try {
    return JSON.parse(sessionStorage.getItem(KEY)) // { username, header }
  } catch {
    return null
  }
}

export function saveAuth(username, password) {
  const header = 'Basic ' + btoa(unescape(encodeURIComponent(`${username}:${password}`)))
  const auth = { username, header }
  sessionStorage.setItem(KEY, JSON.stringify(auth))
  return auth
}

export function clearAuth() {
  sessionStorage.removeItem(KEY)
}

async function request(method, path, body, authOverride) {
  const auth = authOverride ?? loadAuth()
  const res = await fetch('/api' + path, {
    method,
    headers: {
      Accept: 'application/json',
      // Spring antwortet dann mit 401 ohne WWW-Authenticate -> kein Browser-Login-Dialog
      'X-Requested-With': 'XMLHttpRequest',
      ...(auth ? { Authorization: auth.header } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401) {
    if (!authOverride) onUnauthorized()
    throw new ApiError(401, 'Anmeldung fehlgeschlagen oder abgelaufen')
  }
  if (res.status === 403) throw new ApiError(403, 'Dafür fehlt dir die Berechtigung')
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
  login: (username, password) => {
    const auth = saveAuth(username, password)
    return request('GET', '/dashboard', undefined, auth).then(
      () => auth,
      (e) => { clearAuth(); throw e },
    )
  },
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

const money = new Intl.NumberFormat('de-CH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const formatMoney = (amount, currency = 'CHF') => `${currency} ${money.format(amount)}`

const dateTime = new Intl.DateTimeFormat('de-CH', { dateStyle: 'short', timeStyle: 'short', timeZone: 'Europe/Zurich' })
export const formatDateTime = (iso) => (iso ? dateTime.format(new Date(iso)) : '')

export const STATUS_LABEL = { DRAFT: 'Entwurf', PLACED: 'Aufgegeben', SHIPPED: 'Versendet', CANCELLED: 'Storniert' }
