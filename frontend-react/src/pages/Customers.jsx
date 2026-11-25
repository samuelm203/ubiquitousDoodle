import { useMemo, useState } from 'react'
import { api } from '../api.js'
import { useLoad } from '../hooks.js'
import { useToast } from '../toast.jsx'

const EMPTY = { id: null, name: '', email: '', city: '', active: true }

export default function Customers() {
  const toast = useToast()
  const { data: customers, loading, reload } = useLoad(api.customers, [])
  const [filter, setFilter] = useState('')
  const [editing, setEditing] = useState(null)

  const visible = useMemo(() => {
    const q = filter.trim().toLowerCase()
    return customers.filter((c) => !q || `${c.name} ${c.email} ${c.city}`.toLowerCase().includes(q))
  }, [customers, filter])

  async function save(e) {
    e.preventDefault()
    const { id, ...form } = editing
    try {
      await (id ? api.updateCustomer(id, form) : api.createCustomer(form))
      toast.success('Kunde gespeichert')
      setEditing(null)
      reload()
    } catch (err) {
      toast.error(err)
    }
  }

  async function remove() {
    if (!window.confirm(`Kunde „${editing.name}“ wirklich löschen?`)) return
    try {
      await api.deleteCustomer(editing.id)
      toast.success('Kunde gelöscht')
      setEditing(null)
      reload()
    } catch (err) {
      toast.error(err)
    }
  }

  const set = (field) => (e) =>
    setEditing({ ...editing, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  return (
    <div className="split">
      <section>
        <div className="toolbar">
          <input placeholder="Suchen ..." value={filter} onChange={(e) => setFilter(e.target.value)} />
          <button className="btn primary" onClick={() => setEditing({ ...EMPTY })}>Neuer Kunde</button>
        </div>
        {loading ? <p className="muted">Lädt ...</p> : (
          <table>
            <thead><tr><th>Name</th><th>E-Mail</th><th>Ort</th><th>Aktiv</th></tr></thead>
            <tbody>
              {visible.map((c) => (
                <tr key={c.id} className={editing?.id === c.id ? 'selected' : ''} onClick={() => setEditing({ ...c })}>
                  <td>{c.name}</td><td>{c.email}</td><td>{c.city}</td><td>{c.active ? 'Ja' : 'Nein'}</td>
                </tr>
              ))}
              {visible.length === 0 && <tr><td colSpan="4" className="muted">Keine Kunden</td></tr>}
            </tbody>
          </table>
        )}
      </section>

      {editing && (
        <form className="card side" onSubmit={save}>
          <h3>{editing.id ? 'Kunde bearbeiten' : 'Neuer Kunde'}</h3>
          <label>Name<input value={editing.name} onChange={set('name')} required autoFocus /></label>
          <label>E-Mail<input type="email" value={editing.email} onChange={set('email')} required /></label>
          <label>Ort<input value={editing.city ?? ''} onChange={set('city')} /></label>
          <label className="check"><input type="checkbox" checked={editing.active} onChange={set('active')} /> Aktiv</label>
          <div className="row">
            <button className="btn primary">Speichern</button>
            <button type="button" className="btn" onClick={() => setEditing(null)}>Abbrechen</button>
            {editing.id && <button type="button" className="btn danger" onClick={remove}>Löschen</button>}
          </div>
        </form>
      )}
    </div>
  )
}
