<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { attempt } from '../toast.svelte.js'
  import { money } from '../format.js'
  import Modal from '../components/Modal.svelte'

  let products = $state([])
  let filter = $state('')
  let editing = $state(null)
  let form = $state({ sku: '', name: '', price: '', active: true })

  async function load() {
    products = (await attempt(api.products)) ?? []
  }
  onMount(load)

  const visible = $derived.by(() => {
    const q = filter.trim().toLowerCase()
    const list = products.toSorted((a, b) => a.name.localeCompare(b.name, 'de'))
    return q ? list.filter((p) => [p.name, p.sku].some((v) => v?.toLowerCase().includes(q))) : list
  })

  function open(p) {
    editing = p ?? {}
    form = p
      ? { sku: p.sku, name: p.name, price: String(p.price), active: p.active }
      : { sku: '', name: '', price: '', active: true }
  }

  async function save(e) {
    e.preventDefault()
    const id = editing.id
    // Preis als String schicken, damit BigDecimal im Backend exakt bleibt (z. B. "12.50")
    const body = { ...form, price: String(form.price).replace(',', '.') }
    const ok = await attempt(
      () => (id ? api.updateProduct(id, body) : api.createProduct(body)),
      id ? 'Produkt gespeichert' : 'Produkt angelegt',
    )
    if (ok) {
      editing = null
      load()
    }
  }
</script>

<div class="page-head">
  <h1>Produkte</h1>
  <div class="toolbar">
    <input bind:value={filter} type="search" placeholder="Suchen …" aria-label="Produkte suchen" />
    <button class="btn btn-primary" onclick={() => open()}>+ Neues Produkt</button>
  </div>
</div>
<p class="muted note">Produkte anlegen und ändern darf nur ein Admin.</p>

<div class="card table-wrap">
  <table>
    <thead><tr><th>SKU</th><th>Name</th><th class="num">Preis</th><th>Status</th></tr></thead>
    <tbody>
      {#each visible as p (p.id)}
        <tr class="clickable" class:inactive={!p.active} onclick={() => open(p)}>
          <td><code>{p.sku}</code></td>
          <td><strong>{p.name}</strong></td>
          <td class="num">{money(p.price, p.currency)}</td>
          <td><span class="badge {p.active ? 'badge-success' : 'badge-neutral'}">{p.active ? 'Aktiv' : 'Inaktiv'}</span></td>
        </tr>
      {:else}
        <tr><td colspan="4" class="empty">Keine Produkte gefunden</td></tr>
      {/each}
    </tbody>
  </table>
</div>

{#if editing}
  <Modal title={editing.id ? 'Produkt bearbeiten' : 'Neues Produkt'} onclose={() => (editing = null)}>
    <form class="form-grid" onsubmit={save}>
      <label class="field">
        <span>SKU{editing.id ? ' (nicht änderbar)' : ''}</span>
        <input bind:value={form.sku} disabled={!!editing.id} required />
      </label>
      <label class="field"><span>Name</span><input bind:value={form.name} required /></label>
      <label class="field">
        <span>Preis (CHF)</span>
        <input bind:value={form.price} inputmode="decimal" pattern={'\\d+([.,]\\d{1,2})?'} required />
      </label>
      <label class="check"><input bind:checked={form.active} type="checkbox" /> Aktiv</label>
      <div class="form-actions">
        <button type="button" class="btn" onclick={() => (editing = null)}>Abbrechen</button>
        <button class="btn btn-primary">Speichern</button>
      </div>
    </form>
  </Modal>
{/if}

<style>
  .note { margin: -10px 0 16px; font-size: 0.9rem; }
</style>
