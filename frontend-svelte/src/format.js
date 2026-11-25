const moneyFormats = new Map()

export function money(amount, currency = 'CHF') {
  if (amount === null || amount === undefined) return ''
  if (!moneyFormats.has(currency)) {
    moneyFormats.set(currency, new Intl.NumberFormat('de-CH', { style: 'currency', currency }))
  }
  return moneyFormats.get(currency).format(Number(amount))
}

const dateTimeFormat = new Intl.DateTimeFormat('de-CH', { dateStyle: 'short', timeStyle: 'short' })

export const dateTime = (iso) => (iso ? dateTimeFormat.format(new Date(iso)) : '')

export const STATUS = {
  DRAFT: { label: 'Entwurf', tone: 'neutral' },
  PLACED: { label: 'Aufgegeben', tone: 'info' },
  SHIPPED: { label: 'Versendet', tone: 'success' },
  CANCELLED: { label: 'Storniert', tone: 'danger' },
}
