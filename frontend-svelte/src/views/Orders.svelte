<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { attempt, notify } from '../toast.svelte.js'
  import { navigate } from '../router.svelte.js'
  import { dateTime, money, STATUS } from '../format.js'
  import StatusBadge from '../components/StatusBadge.svelte'

  let { selectedId = null } = $props()

  let orders = $state([])
  let customers = $state([])
  let products = $state([])
  let statusFilter = $state('')
  let newCustomer = $state('')
  let lineProduct = $state('')
  let lineQty = $state(1)
  let busy = $state(false)

  const current = $derived(orders.find((o) => o.id === selectedId) ?? null)
  const isDraft = $derived(current?.status === 'DRAFT')
  const isPlaced = $derived(current?.status === 'PLACED')
  const byName = (a, b) => a.name.localeCompare(b.name, 'de')
  const activeCustomers = $derived(customers.filter((c) => c.active).toSorted(byName))
  const activeProducts = $derived(products.filter((p) => p.active).toSorted(byName))
  const visible = $derived(
    orders
      .filter((o) => !statusFilter || o.status === statusFilter)
      .toSorted((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')),
  )

  onMount(async () => {
    const [o, c, p] = await Promise.all([attempt(api.orders), attempt(api.customers), attempt(api.products)])
    orders = o ?? []
    customers = c ?? []
    products = p ?? []
  })

  // Beim Wechsel der Bestellung das Positionsformular zurücksetzen
  $effect(() => {
    selectedId
    lineProduct = ''
    lineQty = 1
  })

  const select = (o) => navigate(o && o.id !== selectedId ? `/orders/${o.id}` : '/orders')

  /** Ersetzt die Bestellung in der Liste durch die Antwort des Servers. */
  function apply(updated) {
    const i = orders.findIndex((o) => o.id === updated.id)
    if (i >= 0) orders[i] = updated
    else orders.push(updated)
  }

  async function run(fn, message) {
    busy = true
    const updated = await attempt(fn, message)
    busy = false
    if (updated) apply(updated)
    return updated
  }

  async function createOrder(e) {
    e.preventDefault()
    if (!newCustomer) return notify('Bitte zuerst einen Kunden wählen', 'error')
    const created = await run(() => api.createOrder(newCustomer), 'Bestellung angelegt')
    if (created) {
      newCustomer = ''
      statusFilter = ''
      navigate(`/orders/${created.id}`)
    }
  }

  async function addLine(e) {
    e.preventDefault()
    const qty = Number(lineQty)
    if (!lineProduct || !Number.isInteger(qty) || qty < 1) return notify('Produkt und Menge (≥ 1) angeben', 'error')
    if (await run(() => api.addLine(current.id, lineProduct, qty))) {
      lineProduct = ''
      lineQty = 1
    }
  }

  const removeLine = (productId) => run(() => api.removeLine(current.id, productId))
  const place = () => run(() => api.place(current.id), 'Bestellung aufgegeben')
  const ship = () => run(() => api.ship(current.id), 'Bestellung versendet')
  function cancel() {
    if (window.confirm(`Bestellung ${current.number} stornieren?`)) {
      run(() => api.cancel(current.id), 'Bestellung storniert')
    }
  }
</script>

<div class="page-head">
  <h1>Bestellungen</h1>
  <form class="toolbar" onsubmit={createOrder}>
    <select bind:value={newCustomer} aria-label="Kunde für neue Bestellung">
      <option value="">Kunde wählen …</option>
      {#each activeCustomers as c (c.id)}<option value={c.id}>{c.name}</option>{/each}
    </select>
    <button class="btn btn-primary" disabled={busy}>+ Neue Bestellung</button>
  </form>
</div>

<div class="layout" class:with-detail={current}>
  <section class="card">
    <div class="filters">
      <button class="chip" class:on={!statusFilter} onclick={() => (statusFilter = '')}>Alle</button>
      {#each Object.entries(STATUS) as [s, info] (s)}
        <button class="chip" class:on={statusFilter === s} onclick={() => (statusFilter = s)}>{info.label}</button>
      {/each}
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Nr.</th><th>Kunde</th><th>Status</th><th>Erfasst</th><th class="num">Total</th></tr></thead>
        <tbody>
          {#each visible as o (o.id)}
            <tr class="clickable" class:selected={o.id === selectedId} onclick={() => select(o)}>
              <td><code>{o.number}</code></td>
              <td>{o.customerName}</td>
              <td><StatusBadge status={o.status} /></td>
              <td class="muted">{dateTime(o.createdAt)}</td>
              <td class="num">{money(o.total, o.currency)}</td>
            </tr>
          {:else}
            <tr><td colspan="5" class="empty">Keine Bestellungen</td></tr>
          {/each}
        </tbody>
      </table>
    </div>
  </section>

  {#if current}
    <section class="card card-pad detail">
      <header class="detail-head">
        <div>
          <h2>Bestellung {current.number}</h2>
          <p class="muted">{current.customerName}</p>
        </div>
        <button class="btn btn-ghost btn-icon" aria-label="Detail schliessen" onclick={() => select(null)}>✕</button>
      </header>

      <dl class="meta">
        <div><dt>Status</dt><dd><StatusBadge status={current.status} /></dd></div>
        <div><dt>Erfasst</dt><dd>{dateTime(current.createdAt)}</dd></div>
        {#if current.placedAt}<div><dt>Aufgegeben</dt><dd>{dateTime(current.placedAt)}</dd></div>{/if}
      </dl>

      <table class="lines">
        <thead>
          <tr>
            <th>Produkt</th><th class="num">Menge</th><th class="num">Preis</th><th class="num">Total</th>
            {#if isDraft}<th></th>{/if}
          </tr>
        </thead>
        <tbody>
          {#each current.lines as l (l.productId)}
            <tr>
              <td>{l.productName}</td>
              <td class="num">{l.quantity}</td>
              <td class="num">{money(l.unitPrice, current.currency)}</td>
              <td class="num">{money(l.lineTotal, current.currency)}</td>
              {#if isDraft}
                <td class="num">
                  <button class="btn btn-ghost btn-icon btn-danger" disabled={busy} aria-label="Position entfernen" onclick={() => removeLine(l.productId)}>🗑</button>
                </td>
              {/if}
            </tr>
          {:else}
            <tr><td colspan={isDraft ? 5 : 4} class="empty">Noch keine Positionen</td></tr>
          {/each}
        </tbody>
        <tfoot>
          <tr>
            <td colspan="3"><strong>Total</strong></td>
            <td class="num"><strong>{money(current.total, current.currency)}</strong></td>
            {#if isDraft}<td></td>{/if}
          </tr>
        </tfoot>
      </table>

      {#if isDraft}
        <form class="add-line" onsubmit={addLine}>
          <select bind:value={lineProduct} aria-label="Produkt">
            <option value="">Produkt wählen …</option>
            {#each activeProducts as p (p.id)}<option value={p.id}>{p.name} ({money(p.price, p.currency)})</option>{/each}
          </select>
          <input bind:value={lineQty} type="number" min="1" step="1" aria-label="Menge" class="qty" />
          <button class="btn" disabled={busy}>Hinzufügen</button>
        </form>
      {/if}

      {#if isDraft || isPlaced}
        <div class="actions">
          {#if isDraft}<button class="btn btn-primary" disabled={busy} onclick={place}>✓ Aufgeben</button>{/if}
          {#if isPlaced}<button class="btn btn-primary" disabled={busy} onclick={ship}>Versenden</button>{/if}
          <button class="btn btn-danger" disabled={busy} onclick={cancel}>Stornieren</button>
        </div>
      {/if}
    </section>
  {/if}
</div>

<style>
  .layout { display: grid; gap: 16px; grid-template-columns: 1fr; align-items: start; }
  .layout.with-detail { grid-template-columns: minmax(0, 1fr) minmax(360px, 520px); }
  @media (max-width: 1100px) { .layout.with-detail { grid-template-columns: 1fr; } }
  .filters { display: flex; gap: 6px; padding: 12px 14px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
  .chip { font: inherit; font-size: 0.85rem; border: 1px solid var(--border); background: transparent; color: var(--muted); border-radius: 999px; padding: 3px 12px; cursor: pointer; }
  .chip.on { background: var(--primary); border-color: var(--primary); color: #fff; }
  .detail { display: grid; gap: 16px; position: sticky; top: 20px; }
  .detail-head { display: flex; justify-content: space-between; align-items: flex-start; }
  .detail-head p { margin: 4px 0 0; }
  .meta { display: flex; gap: 28px; margin: 0; flex-wrap: wrap; }
  .meta dt { font-size: 0.8rem; color: var(--muted); margin-bottom: 4px; }
  .meta dd { margin: 0; }
  .lines th, .lines td { padding: 8px 6px; }
  .lines tfoot td { border-top: 2px solid var(--border); border-bottom: none; }
  .add-line { display: flex; gap: 8px; }
  .add-line select { flex: 1; }
  .qty { width: 80px; }
  .actions { display: flex; gap: 8px; border-top: 1px solid var(--border); padding-top: 16px; }
</style>
