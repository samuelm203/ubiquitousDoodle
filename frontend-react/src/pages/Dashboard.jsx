import { api, formatMoney } from '../api.js'
import { useLoad } from '../hooks.js'

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span className="muted">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export default function Dashboard() {
  const { data: d, loading } = useLoad(api.dashboard)
  if (loading) return <p className="muted">Lädt ...</p>
  if (!d) return null

  const by = d.ordersByStatus
  return (
    <>
      <h2>Übersicht</h2>
      <p class="muted">Framework: React</p>
      <div className="stats">
        <Stat label="Umsatz" value={formatMoney(d.revenue, d.currency)} />
        <Stat label="Offene Bestellungen" value={by.PLACED ?? 0} />
        <Stat label="Entwürfe" value={by.DRAFT ?? 0} />
        <Stat label="Versendet" value={by.SHIPPED ?? 0} />
        <Stat label="Kunden" value={d.customers} />
        <Stat label="Produkte" value={d.products} />
      </div>
    </>
  )
}
