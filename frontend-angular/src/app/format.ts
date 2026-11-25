const moneyFormats = new Map<string, Intl.NumberFormat>();

export function money(amount: number | string | null | undefined, currency = 'CHF'): string {
  if (amount === null || amount === undefined) return '';
  let fmt = moneyFormats.get(currency);
  if (!fmt) {
    fmt = new Intl.NumberFormat('de-CH', { style: 'currency', currency });
    moneyFormats.set(currency, fmt);
  }
  return fmt.format(Number(amount));
}

const dateTimeFormat = new Intl.DateTimeFormat('de-CH', { dateStyle: 'short', timeStyle: 'short' });

export function dateTime(iso: string | null | undefined): string {
  return iso ? dateTimeFormat.format(new Date(iso)) : '';
}

export type Tone = 'neutral' | 'info' | 'success' | 'danger';
export type OrderStatus = 'DRAFT' | 'PLACED' | 'SHIPPED' | 'CANCELLED';

export const STATUS: Record<OrderStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: 'Entwurf', tone: 'neutral' },
  PLACED: { label: 'Aufgegeben', tone: 'info' },
  SHIPPED: { label: 'Versendet', tone: 'success' },
  CANCELLED: { label: 'Storniert', tone: 'danger' },
};

export const STATUS_LIST = (Object.keys(STATUS) as OrderStatus[]).map((key) => ({ key, ...STATUS[key] }));

export const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name, 'de');
export const newestFirst = (a: { createdAt?: string | null }, b: { createdAt?: string | null }) =>
  (b.createdAt ?? '').localeCompare(a.createdAt ?? '');
