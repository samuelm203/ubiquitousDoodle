import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api.js'
import { useLoad } from '../hooks.js'
import { useToast } from '../toast.jsx'

const SIZES = [50, 100, 200, 500]

// Eine Zeile wird nur neu gezeichnet, wenn sich ihre Daten oder ihre offenen Änderungen ändern.
const Row = memo(function Row({ row, columns, pending, onEdit }) {
  const diff = new Set(row.values).size > 1
  return (
    <tr className={diff ? 'diff' : ''}>
      <td className="key">{row.key}</td>
      {columns.map((col, i) => {
        const changed = pending !== undefined && col in pending
        const value = changed ? pending[col] : row.values[i]
        return (
          <td key={col} className={`cell${changed ? ' changed' : ''}${value === null ? ' missing' : ''}`}>
            <input
              value={value ?? ''}
              placeholder={value === null ? '(fehlt)' : ''}
              aria-label={`${row.key} (${col})`}
              onChange={(e) => onEdit(row.key, col, e.target.value, row.values[i])}
            />
            {value !== null && (
              <button type="button" className="x" title="Schlüssel in dieser Spalte löschen" aria-label="Löschen"
                onClick={() => onEdit(row.key, col, null, row.values[i])}>×</button>
            )}
          </td>
        )
      })}
    </tr>
  )
})

export default function Properties() {
  const toast = useToast()
  const { data: companies, loading } = useLoad(api.propertyCompanies, [])
  const [company, setCompany] = useState('')
  const [filterInput, setFilterInput] = useState('')
  const [filter, setFilter] = useState('')
  const [onlyDifferences, setOnlyDifferences] = useState(false)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(100)
  const [table, setTable] = useState(null)
  const [busy, setBusy] = useState(false)
  // Offene Änderungen: Schlüssel -> { Spalte: neuer Wert | null }, bleiben beim Blättern/Filtern erhalten
  const [pending, setPending] = useState(() => new Map())
  const reqId = useRef(0)

  useEffect(() => { if (!company && companies.length) setCompany(companies[0].code) }, [companies, company])

  // Filter mit Verzögerung an den Server geben
  useEffect(() => {
    const t = setTimeout(() => { setFilter(filterInput.trim()); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [filterInput])

  const load = useCallback(async () => {
    if (!company) return
    const id = ++reqId.current
    try {
      const t = await api.propertyTable(company, { filter, onlyDifferences, page, size })
      if (id === reqId.current) setTable(t) // veraltete Antworten verwerfen
    } catch (e) {
      if (id === reqId.current) toast.error(e)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [company, filter, onlyDifferences, page, size])
  useEffect(() => { load() }, [load])

  const onEdit = useCallback((key, col, value, original) => {
    setPending((prev) => {
      const next = new Map(prev)
      const row = { ...(next.get(key) ?? {}) }
      if (value === original) delete row[col]; else row[col] = value
      if (Object.keys(row).length) next.set(key, row); else next.delete(key)
      return next
    })
  }, [])

  const count = useMemo(() => { let n = 0; pending.forEach((r) => { n += Object.keys(r).length }); return n }, [pending])

  function changeCompany(code) {
    if (count && !window.confirm('Nicht gespeicherte Änderungen verwerfen?')) return
    setPending(new Map()); setTable(null); setPage(0); setCompany(code)
  }

  async function save() {
    const changes = []
    pending.forEach((cols, key) => Object.entries(cols).forEach(([column, value]) => changes.push({ column, key, value })))
    setBusy(true)
    try {
      const r = await api.changeProperties(company, changes)
      toast.success(`${r.applied} Änderung(en) gespeichert`)
      setPending(new Map())
      load()
    } catch (e) {
      toast.error(e) // alles oder nichts: die Änderungen bleiben zum Korrigieren erhalten
    } finally {
      setBusy(false)
    }
  }

  const pages = table ? Math.max(1, Math.ceil(table.total / table.size)) : 1

  if (loading) return <p className="muted">Lädt ...</p>
  return (
    <>
      <h2>Firmenkonfiguration</h2>
      <div className="toolbar">
        <select value={company} onChange={(e) => changeCompany(e.target.value)} aria-label="Firma">
          {companies.map((c) => <option key={c.code} value={c.code}>{c.code} ({c.keyCount})</option>)}
        </select>
        <input placeholder="Schlüssel filtern ..." value={filterInput} onChange={(e) => setFilterInput(e.target.value)} />
        <label className="check inline">
          <input type="checkbox" checked={onlyDifferences}
            onChange={(e) => { setOnlyDifferences(e.target.checked); setPage(0) }} /> Nur Unterschiede
        </label>
        <span className="spacer" />
        <span className="muted">{count} offene Änderung(en)</span>
        <button className="btn" disabled={!count || busy} onClick={() => setPending(new Map())}>Verwerfen</button>
        <button className="btn primary" disabled={!count || busy} onClick={save}>Speichern</button>
      </div>

      {!table ? <p className="muted">Lädt ...</p> : (
        <>
          <table className="props">
            <thead><tr><th>Schlüssel</th>{table.columns.map((c) => <th key={c}>{c}</th>)}</tr></thead>
            <tbody>
              {table.rows.map((r) => (
                <Row key={r.key} row={r} columns={table.columns} pending={pending.get(r.key)} onEdit={onEdit} />
              ))}
              {table.rows.length === 0 && <tr><td colSpan={table.columns.length + 1} className="muted">Keine Einträge</td></tr>}
            </tbody>
          </table>
          <div className="toolbar pager">
            <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>Zurück</button>
            <span>Seite {table.page + 1} / {pages} ({table.total} Zeilen)</span>
            <button className="btn" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Weiter</button>
            <span className="spacer" />
            <select value={size} onChange={(e) => { setSize(+e.target.value); setPage(0) }} aria-label="Zeilen pro Seite">
              {SIZES.map((s) => <option key={s} value={s}>{s} pro Seite</option>)}
            </select>
          </div>
        </>
      )}
    </>
  )
}
