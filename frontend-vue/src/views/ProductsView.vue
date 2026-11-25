<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api.js'
import { attempt } from '../notify.js'
import { money } from '../format.js'
import ModalDialog from '../components/ModalDialog.vue'

const products = ref([])
const filter = ref('')
const editing = ref(null)
const form = reactive({ sku: '', name: '', price: '', active: true })

async function load() {
  products.value = (await attempt(api.products.list)) ?? []
}
onMounted(load)

const visible = computed(() => {
  const q = filter.value.trim().toLowerCase()
  const list = products.value.slice().sort((a, b) => a.name.localeCompare(b.name, 'de'))
  if (!q) return list
  return list.filter((p) => [p.name, p.sku].some((v) => v?.toLowerCase().includes(q)))
})

function open(product) {
  editing.value = product ?? {}
  Object.assign(form, product
    ? { sku: product.sku, name: product.name, price: String(product.price), active: product.active }
    : { sku: '', name: '', price: '', active: true })
}

async function save() {
  const id = editing.value.id
  // Preis als String schicken, damit BigDecimal im Backend exakt bleibt (z. B. "12.50")
  const body = { ...form, price: String(form.price).replace(',', '.') }
  const ok = await attempt(
    () => (id ? api.products.update(id, body) : api.products.create(body)),
    id ? 'Produkt gespeichert' : 'Produkt angelegt',
  )
  if (ok) {
    editing.value = null
    load()
  }
}
</script>

<template>
  <div class="page-head">
    <h1>Produkte</h1>
    <div class="toolbar">
      <input v-model="filter" type="search" placeholder="Suchen …" aria-label="Produkte suchen" />
      <button class="btn btn-primary" @click="open()">+ Neues Produkt</button>
    </div>
  </div>
  <p class="muted note">Produkte anlegen und ändern darf nur ein Admin.</p>

  <div class="card table-wrap">
    <table>
      <thead>
        <tr><th>SKU</th><th>Name</th><th class="num">Preis</th><th>Status</th></tr>
      </thead>
      <tbody>
        <tr v-for="p in visible" :key="p.id" class="clickable" :class="{ inactive: !p.active }" @click="open(p)">
          <td><code>{{ p.sku }}</code></td>
          <td><strong>{{ p.name }}</strong></td>
          <td class="num">{{ money(p.price, p.currency) }}</td>
          <td><span class="badge" :class="p.active ? 'badge-success' : 'badge-neutral'">{{ p.active ? 'Aktiv' : 'Inaktiv' }}</span></td>
        </tr>
        <tr v-if="!visible.length"><td colspan="4" class="empty">Keine Produkte gefunden</td></tr>
      </tbody>
    </table>
  </div>

  <ModalDialog v-if="editing" :title="editing.id ? 'Produkt bearbeiten' : 'Neues Produkt'" @close="editing = null">
    <form class="form-grid" @submit.prevent="save">
      <label class="field">
        <span>SKU{{ editing.id ? ' (nicht änderbar)' : '' }}</span>
        <input v-model="form.sku" :disabled="!!editing.id" required autofocus />
      </label>
      <label class="field"><span>Name</span><input v-model="form.name" required /></label>
      <label class="field"><span>Preis (CHF)</span><input v-model="form.price" inputmode="decimal" pattern="\d+([.,]\d{1,2})?" required /></label>
      <label class="check"><input v-model="form.active" type="checkbox" /> Aktiv</label>
      <div class="form-actions">
        <button type="button" class="btn" @click="editing = null">Abbrechen</button>
        <button class="btn btn-primary">Speichern</button>
      </div>
    </form>
  </ModalDialog>
</template>

<style scoped>
.note { margin: -10px 0 16px; font-size: 0.9rem; }
</style>
