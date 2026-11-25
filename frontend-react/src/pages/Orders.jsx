import { useEffect, useState } from 'react'
import { api, formatDateTime, formatMoney, STATUS_LABEL } from '../api.js'
import { useLoad } from '../hooks.js'
import { useToast } from '../toast.jsx'

function StatusBadge({ status }) {
  return <span className={`badge ${status.toLowerCase()}`}>{STATUS_LABEL[status] ?? status}</span>
}

export default function Orders() {
  const toast = useToast()
  const { data: orders, loading, reload } = useLoad(api.orders, [])
  const { data: customers } = useLoad(api.customers, [])
  const { data: products } = useLoad(api.products, [])

  const [selectedId, setSelectedId] = useState(null)
  const [newCustomer, setNewCustomer] = useState('')
  const [lineProduct, setLineProduct] = useState('')
  const [lineQty, setLineQty] = useState(1)

  const selected = orders.find((o) => o.id === selectedId) ?? null
  useEffect(() => { setLineProduct(''); setLineQty(1) }, [selectedId])

  /** Führt eine Aktion aus, lädt die Liste neu und behält die Auswahl bei. */
  async function act(action, successMessage) {
    try {
      const updated = await action()
      if (successMessage) toast.success(successMessage)
      await reload()
      if (updated?.id) setSelectedId(updated.id)
    } catch (err) {
      toast.error(err)
    }
  }

  const create = () => {
    if (!newCustomer) return toast.error(new Error('Bitte zuerst einen Kunden wählen'))
    act(() => api.createOrder(newCustomer), 'Entwurf angelegt')
  }

  const addLine = () => {
    if (!lineProduct) return toast.error(new Error('Bitte ein Produkt wählen'))
    act(() => api.addLine(selected.id, lineProduct, Number(lineQty)))
    setLineProduct('')
    setLineQty(1)
  }

  const draft = selected?.status === 'DRAFT'

  return (
    <div className="split">
      <section>
        <div className="toolbar">
          <select value={newCustomer} onChange={(e) => setNewCustomer(e.target.value)}>
            <option value="">Kunde wählen ...</option>
            {customers.filter((c) => c.active).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button className="btn primary" onClick={create}>Neue Bestellung</button>
        </div>
        {loading ? <p className="muted">Lädt ...</p> : (
          <table>
            <thead><tr><th>Nr.</th><th>Kunde</th><th>Status</th><th>Erfasst</th><th className="num">Total</th></tr></thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.id} className={o.id === selectedId ? 'selected' : ''} onClick={() => setSelectedId(o.id)}>
                  <td>{o.number}</td>
                  <td>{o.customerName}</td>
                  <td><StatusBadge status={o.status} /></td>
                  <td>{formatDateTime(o.createdAt)}</td>
                  <td className="num">{formatMoney(o.total, o.currency)}</td>
                </tr>
              ))}
              {orders.length === 0 && <tr><td colSpan="5" className="muted">Keine Bestellungen</td></tr>}
            </tbody>
          </table>
        )}
      </section>

      {selected && (
        <div className="card side wide">
          <h3>Bestellung {selected.number} · {selected.customerName}</h3>
          <p className="muted">
            <StatusBadge status={selected.status} />
            {selected.placedAt && <> · aufgegeben {formatDateTime(selected.placedAt)}</>}
          </p>

          <table>
            <thead><tr><th>Produkt</th><th className="num">Menge</th><th className="num">Preis</th><th className="num">Total</th>{draft && <th />}</tr></thead>
            <tbody>
              {selected.lines.map((l) => (
                <tr key={l.productId}>
                  <td>{l.productName}</td>
                  <td className="num">{l.quantity}</td>
                  <td className="num">{formatMoney(l.unitPrice, selected.currency)}</td>
                  <td className="num">{formatMoney(l.lineTotal, selected.currency)}</td>
                  {draft && (
                    <td>
                      <button className="btn ghost danger" aria-label="Position entfernen"
                        onClick={() => act(() => api.removeLine(selected.id, l.productId))}>✕</button>
                    </td>
                  )}
                </tr>
              ))}
              {selected.lines.length === 0 && <tr><td colSpan="5" className="muted">Noch keine Positionen</td></tr>}
            </tbody>
            <tfoot><tr><th colSpan="3">Total</th><th className="num">{formatMoney(selected.total, selected.currency)}</th>{draft && <th />}</tr></tfoot>
          </table>

          {draft && (
            <div className="toolbar">
              <select value={lineProduct} onChange={(e) => setLineProduct(e.target.value)}>
                <option value="">Produkt wählen ...</option>
                {products.filter((p) => p.active).map((p) => (
                  <option key={p.id} value={p.id}>{p.name} ({formatMoney(p.price, p.currency)})</option>
                ))}
              </select>
              <input type="number" min="1" max="1000" value={lineQty} style={{ width: '5rem' }}
                onChange={(e) => setLineQty(e.target.value)} />
              <button className="btn" onClick={addLine}>Hinzufügen</button>
            </div>
          )}

          <div className="row">
            {draft && <button className="btn primary" onClick={() => act(() => api.place(selected.id), 'Bestellung aufgegeben')}>Aufgeben</button>}
            {selected.status === 'PLACED' && <button className="btn primary" onClick={() => act(() => api.ship(selected.id), 'Bestellung versendet')}>Versenden</button>}
            {(draft || selected.status === 'PLACED') && (
              <button className="btn danger" onClick={() => window.confirm('Bestellung stornieren?') && act(() => api.cancel(selected.id), 'Bestellung storniert')}>Stornieren</button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
