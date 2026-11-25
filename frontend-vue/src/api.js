import { auth, logout } from './auth.js'
import { router } from './router.js'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(method, path, body) {
  const headers = {
    Accept: 'application/json',
    // Ohne diesen Header schickt Spring Security bei 401 "WWW-Authenticate: Basic",
    // und der Browser öffnet seinen eigenen Login-Dialog.
    'X-Requested-With': 'XMLHttpRequest',
  }
  if (auth.token) headers.Authorization = `Basic ${auth.token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Server nicht erreichbar')
  }

  if (res.status === 401) {
    logout()
    if (router.currentRoute.value.name !== 'login') {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    throw new ApiError(401, 'Benutzername oder Passwort falsch')
  }
  if (res.status === 403) throw new ApiError(403, 'Dafür fehlt die Berechtigung (nur Admin)')
  if (res.status === 204) return null

  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    // RFC-9457 Problem Details: fachliche Meldung steht in "detail"
    throw new ApiError(res.status, data?.detail || data?.title || `Fehler ${res.status}`)
  }
  return data
}

export const api = {
  dashboard: () => request('GET', '/dashboard'),

  customers: {
    list: () => request('GET', '/customers'),
    create: (form) => request('POST', '/customers', form),
    update: (id, form) => request('PUT', `/customers/${id}`, form),
    remove: (id) => request('DELETE', `/customers/${id}`),
  },

  products: {
    list: () => request('GET', '/products'),
    create: (form) => request('POST', '/products', form),
    update: (id, form) => request('PUT', `/products/${id}`, form),
  },

  orders: {
    list: () => request('GET', '/orders'),
    get: (id) => request('GET', `/orders/${id}`),
    create: (customerId) => request('POST', '/orders', { customerId }),
    addLine: (id, productId, quantity) => request('POST', `/orders/${id}/lines`, { productId, quantity }),
    removeLine: (id, productId) => request('DELETE', `/orders/${id}/lines/${productId}`),
    place: (id) => request('POST', `/orders/${id}/place`),
    ship: (id) => request('POST', `/orders/${id}/ship`),
    cancel: (id) => request('POST', `/orders/${id}/cancel`),
  },

  // nur ADMIN
  properties: {
    companies: () => request('GET', '/properties/companies'),
    table: (company, { filter = '', onlyDifferences = false, page = 0, size = 100 } = {}) => {
      const q = new URLSearchParams({ page, size })
      if (filter) q.set('filter', filter)
      if (onlyDifferences) q.set('onlyDifferences', 'true')
      return request('GET', `/properties/${encodeURIComponent(company)}?${q}`)
    },
    change: (company, changes) => request('PUT', `/properties/${encodeURIComponent(company)}`, { changes }),
  },
}
