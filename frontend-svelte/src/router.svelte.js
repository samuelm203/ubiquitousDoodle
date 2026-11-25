// Minimaler Hash-Router (#/orders/<id>): braucht keine Server-Konfiguration für Deep Links.
function read() {
  return location.hash.replace(/^#/, '') || '/'
}

export const route = $state({ path: read() })

window.addEventListener('hashchange', () => {
  route.path = read()
})

export function navigate(path, { replace = false } = {}) {
  if (replace) location.replace('#' + path)
  else location.hash = path
}
