<script>
  import { onMount } from 'svelte'
  import { api } from '../api.js'
  import { attempt } from '../toast.svelte.js'
  import Modal from '../components/Modal.svelte'

  let customers = $state([])
  let filter = $state('')
  let editing = $state(null) // null = zu, {} = neu, {id,...} = bearbeiten
  let form = $state({ name: '', email: '', city: '', active: true })

  async function load() {
    customers = (await attempt(api.customers)) ?? []
  }
  onMount(load)

  const visible = $derived.by(() => {
    const q = filter.trim().toLowerCase()
    const list = customers.toSorted((a, b) => a.name.localeCompare(b.name, 'de'))
    return q ? list.filter((c) => [c.name, c.email, c.city].some((v) => v?.toLowerCase().includes(q))) : list
  })

  function open(c) {
    editing = c ?? {}
    form = c
      ? { name: c.name, email: c.email, city: c.city ?? '', active: c.active }
      : { name: '', email: '', city: '', active: true }
  }

  async function save(e) {
    e.preventDefault()
    const id = editing.id
    const body = { ...form, city: form.city || null }
    const ok = await attempt(
      () => (id ? api.updateCustomer(id, body) : api.createCustomer(body)),
      id ? 'Kunde gespeichert' : 'Kunde angelegt',
    )
    if (ok) {
      editing = null
      load()
    }
  }

  async function remove() {
    const c = editing
    if (!window.confirm(`Kunde «${c.name}» wirklich löschen?`)) return
    const ok = await attempt(() => api.deleteCustomer(c.id).then(() => true), 'Kunde gelöscht')
    if (ok) {
      editing = null
      load()
    }
  }
</script>

<div class="page-head">
  <h1>Kunden</h1>
  <div class="toolbar">
    <input bind:value={filter} type="search" placeholder="Suchen …" aria-label="Kunden suchen" />
    <button class="btn btn-primary" onclick={() => open()}>+ Neuer Kunde</button>
  </div>
</div>

<div class="card table-wrap">
  <table>
    <thead><tr><th>Name</th><th>E-Mail</th><th>Ort</th><th>Status</th></tr></thead>
    <tbody>
      {#each visible as c (c.id)}
        <tr class="clickable" class:inactive={!c.active} onclick={() => open(c)}>
          <td><strong>{c.name}</strong></td>
          <td>{c.email}</td>
          <td>{c.city ?? ''}</td>
          <td><span class="badge {c.active ? 'badge-success' : 'badge-neutral'}">{c.active ? 'Aktiv' : 'Inaktiv'}</span></td>
        </tr>
      {:else}
        <tr><td colspan="4" class="empty">Keine Kunden gefunden</td></tr>
      {/each}
    </tbody>
  </table>
</div>

{#if editing}
  <Modal title={editing.id ? 'Kunde bearbeiten' : 'Neuer Kunde'} onclose={() => (editing = null)}>
    <form class="form-grid" onsubmit={save}>
      <label class="field"><span>Name</span><input bind:value={form.name} required /></label>
      <label class="field"><span>E-Mail</span><input bind:value={form.email} type="email" required /></label>
      <label class="field"><span>Ort</span><input bind:value={form.city} /></label>
      <label class="check"><input bind:checked={form.active} type="checkbox" /> Aktiv</label>
      <div class="form-actions">
        {#if editing.id}
          <button type="button" class="btn btn-danger" style="margin-right: auto" onclick={remove}>Löschen</button>
        {/if}
        <button type="button" class="btn" onclick={() => (editing = null)}>Abbrechen</button>
        <button class="btn btn-primary">Speichern</button>
      </div>
    </form>
  </Modal>
{/if}
