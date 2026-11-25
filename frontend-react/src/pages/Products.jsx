import { useState } from 'react'
import { api, formatMoney } from '../api.js'
import { useLoad } from '../hooks.js'
import { useToast } from '../toast.jsx'

const EMPTY = { id: null, sku: '', name: '', price: '', active: true }

export default function Products() {
  const toast = useToast()
  const { data: products, loading, reload } = useLoad(api.products, [])
  const [editing, setEditing] = useState(null)

  async function save(e) {
    e.preventDefault()
    const { id, sku, name, price, active } = editing
    const form = { sku, name, price: Number(price), active }
    try {
      await (id ? api.updateProduct(id, form) : api.createProduct(form))
      toast.success('Produkt gespeichert')
      setEditing(null)
      reload()
    } catch (err) {
      toast.error(err) // z. B. 403, wenn der Benutzer kein ADMIN ist
    }
  }

  const set = (field) => (e) =>
    setEditing({ ...editing, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  return (
    <div className="split">
      <section>
        <div className="toolbar">
          <button className="btn primary" onClick={() => setEditing({ ...EMPTY })}>Neues Produkt</button>
          <span className="muted">Ändern dürfen nur Administratoren.</span>
        </div>
        {loading ? <p className="muted">Lädt ...</p> : (
          <table>
            <thead><tr><th>SKU</th><th>Name</th><th className="num">Preis</th><th>Lieferbar</th></tr></thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className={editing?.id === p.id ? 'selected' : ''} onClick={() => setEditing({ ...p })}>
                  <td>{p.sku}</td><td>{p.name}</td>
                  <td className="num">{formatMoney(p.price, p.currency)}</td>
                  <td>{p.active ? 'Ja' : 'Nein'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editing && (
        <form className="card side" onSubmit={save}>
          <h3>{editing.id ? 'Produkt bearbeiten' : 'Neues Produkt'}</h3>
          <label>SKU<input value={editing.sku} onChange={set('sku')} required readOnly={!!editing.id} /></label>
          <label>Name<input value={editing.name} onChange={set('name')} required autoFocus /></label>
          <label>Preis (CHF)
            <input type="number" min="0" step="0.05" value={editing.price} onChange={set('price')} required />
          </label>
          <label className="check"><input type="checkbox" checked={editing.active} onChange={set('active')} /> Lieferbar</label>
          <div className="row">
            <button className="btn primary">Speichern</button>
            <button type="button" className="btn" onClick={() => setEditing(null)}>Abbrechen</button>
          </div>
        </form>
      )}
    </div>
  )
}
