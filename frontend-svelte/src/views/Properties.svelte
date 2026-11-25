<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { attempt, notify } from '../toast.svelte.js'

  const SIZE = 100 // ~1000 Schlüssel: serverseitig seitenweise laden hält das DOM klein

  let companies = $state([])
  let company = $state('')
  let filter = $state('')
  let onlyDifferences = $state(false)
  let page = $state(0)
  let table = $state.raw(null) // {company, columns, rows:[{key,values}], total, size}; roh = kein Proxy pro Zelle
  let pending = $state({}) // "spalte\0schlüssel" -> neuer Wert (null = löschen); bleibt über Seiten/Filter erhalten
  let loading = $state(false)
  let saving = $state(false)

  const id = (col, key) => col + '\0' + key
  const pendingCount = $derived(Object.keys(pending).length)
  const pages = $derived(table ? Math.max(1, Math.ceil(table.total / table.size)) : 1)

  let seq = 0
  async function load() {
    if (!company) return
    const my = ++seq
    loading = true
    const t = await attempt(() => api.propertyTable(company, { filter: filter.trim(), onlyDifferences, page, size: SIZE }))
    if (my !== seq) return // veraltete Antwort
    loading = false
    if (t) table = t
  }

  onMount(async () => {
    companies = (await attempt(api.propertyCompanies)) ?? []
    if (companies.length) {
      company = companies[0].code
      load()
    }
  })

  // Filter entprellt; jede Änderung von Filter/Auswahl springt auf Seite 1
  let timer
  function refilter(delay = 0) {
    clearTimeout(timer)
    timer = setTimeout(() => { page = 0; load() }, delay)
  }

  function selectCompany() {
    if (pendingCount && !window.confirm('Ungespeicherte Änderungen verwerfen?')) {
      company = table.company
      return
    }
    pending = {}
    filter = ''
    onlyDifferences = false
    refilter()
  }

  function go(p) {
    page = p
    load()
  }

  const isDiff = (row) => row.values.some((v) => v !== row.values[0])

  function edit(row, i, text) {
    const k = id(table.columns[i], row.key)
    const orig = row.values[i]
    if (text === (orig ?? '')) delete pending[k] // unverändert (oder fehlend und leer gelassen)
    else pending[k] = text
  }

  function remove(row, i) {
    const k = id(table.columns[i], row.key)
    if (k in pending) delete pending[k] // Änderung zurücknehmen
    else pending[k] = null
  }

  async function save() {
    const changes = Object.entries(pending).map(([k, value]) => {
      const [column, key] = k.split('\0')
      return { column, key, value }
    })
    saving = true
    const res = await attempt(() => api.changeProperties(company, changes))
    saving = false
    if (res) {
      notify(`${res.applied} Änderung(en) gespeichert`)
      pending = {}
      load()
    }
  }
</script>

<div class="page-head">
  <h1>Konfiguration</h1>
  <div class="toolbar">
    <select bind:value={company} onchange={selectCompany} aria-label="Firma">
      {#each companies as c (c.code)}<option value={c.code}>{c.code} ({c.keyCount})</option>{/each}
    </select>
    <input bind:value={filter} oninput={() => refilter(300)} type="search" placeholder="Schlüssel filtern …" aria-label="Schlüssel filtern" />
    <label class="check"><input type="checkbox" bind:checked={onlyDifferences} onchange={() => refilter()} /> Nur Unterschiede</label>
    <button class="btn btn-primary" disabled={!pendingCount || saving} onclick={save}>
      Speichern{pendingCount ? ` (${pendingCount})` : ''}
    </button>
    <button class="btn" disabled={!pendingCount || saving} onclick={() => (pending = {})}>Verwerfen</button>
  </div>
</div>

<div class="card table-wrap props" aria-busy={loading}>
  <table>
    <thead>
      <tr><th>Schlüssel</th>{#each table?.columns ?? [] as col (col)}<th>{col}</th>{/each}</tr>
    </thead>
    <tbody>
      {#if table}
        {#each table.rows as row (row.key)}
          <tr class:diff={isDiff(row)}>
            <td class="key">{row.key}</td>
            {#each table.columns as col, i (col)}
              {@const k = id(col, row.key)}
              {@const changed = k in pending}
              {@const v = changed ? pending[k] : row.values[i]}
              <td>
                <div class="cell" class:missing={v === null} class:dirty={changed && v !== null} class:deleted={changed && v === null}>
                  <input
                    value={v ?? ''}
                    placeholder={v === null ? '(fehlt)' : ''}
                    aria-label="{row.key} {col}"
                    onchange={(e) => edit(row, i, e.currentTarget.value)}
                  />
                  <button
                    class="btn btn-ghost btn-icon"
                    title={changed ? 'Änderung zurücknehmen' : 'Schlüssel in dieser Spalte löschen'}
                    disabled={row.values[i] === null && !changed}
                    onclick={() => remove(row, i)}
                  >{changed ? '↺' : '×'}</button>
                </div>
              </td>
            {/each}
          </tr>
        {:else}
          <tr><td colspan={table.columns.length + 1} class="empty">Keine Schlüssel gefunden</td></tr>
        {/each}
      {:else}
        <tr><td colspan="5" class="empty">{loading ? 'Lädt …' : 'Keine Firma'}</td></tr>
      {/if}
    </tbody>
  </table>
  {#if table}
    <div class="pager muted">
      <span>{table.total} Schlüssel</span>
      <button class="btn" disabled={page === 0} onclick={() => go(page - 1)}>‹</button>
      <span>Seite {page + 1} / {pages}</span>
      <button class="btn" disabled={page + 1 >= pages} onclick={() => go(page + 1)}>›</button>
    </div>
  {/if}
</div>
