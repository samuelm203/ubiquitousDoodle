import { reactive } from 'vue'

export const toasts = reactive([])
let nextId = 1

export function notify(message, kind = 'success') {
  const id = nextId++
  toasts.push({ id, message, kind })
  setTimeout(() => {
    const i = toasts.findIndex((t) => t.id === id)
    if (i >= 0) toasts.splice(i, 1)
  }, kind === 'error' ? 6000 : 3000)
}

/** Führt einen API-Aufruf aus und zeigt Fehler als Toast. Gibt bei Fehler undefined zurück. */
export async function attempt(fn, successMessage) {
  try {
    const result = await fn()
    if (successMessage) notify(successMessage)
    return result
  } catch (e) {
    if (e.status !== 401) notify(e.message, 'error')
    return undefined
  }
}
