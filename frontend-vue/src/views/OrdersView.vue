<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api.js'
import { attempt, notify } from '../notify.js'
import { dateTime, money, STATUS } from '../format.js'
import StatusBadge from '../components/StatusBadge.vue'

const route = useRoute()
const router = useRouter()

const orders = ref([])
const customers = ref([])
const products = ref([])
const statusFilter = ref('')
const newCustomer = ref('')
const lineProduct = ref('')
const lineQty = ref(1)
const busy = ref(false)

const selectedId = computed(() => route.params.id || null)
const current = computed(() => orders.value.find((o) => o.id === selectedId.value) ?? null)
const activeCustomers = computed(() => customers.value.filter((c) => c.active).sort((a, b) => a.name.localeCompare(b.name, 'de')))
const activeProducts = computed(() => products.value.filter((p) => p.active).sort((a, b) => a.name.localeCompare(b.name, 'de')))

const visible = computed(() =>
  orders.value
    .filter((o) => !statusFilter.value || o.status === statusFilter.value)
    .slice()
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')),
)

async function loadOrders() {
  orders.value = (await attempt(api.orders.list)) ?? []
}

onMounted(async () => {
  const [o, c, p] = await Promise.all([attempt(api.orders.list), attempt(api.customers.list), attempt(api.products.list)])
  orders.value = o ?? []
  customers.value = c ?? []
  products.value = p ?? []
})

watch(selectedId, () => {
  lineProduct.value = ''
  lineQty.value = 1
})

function select(o) {
  router.replace(o && o.id !== selectedId.value ? `/orders/${o.id}` : '/orders')
}

/** Ersetzt die Bestellung in der Liste durch die Antwort des Servers. */
function apply(updated) {
  const i = orders.value.findIndex((o) => o.id === updated.id)
  if (i >= 0) orders.value.splice(i, 1, updated)
  else orders.value.push(updated)
}

async function run(fn, message) {
  busy.value = true
  const updated = await attempt(fn, message)
  busy.value = false
  if (updated) apply(updated)
  return updated
}

async function createOrder() {
  if (!newCustomer.value) {
    notify('Bitte zuerst einen Kunden wählen', 'error')
    return
  }
  const created = await run(() => api.orders.create(newCustomer.value), 'Bestellung angelegt')
  if (created) {
    newCustomer.value = ''
    statusFilter.value = ''
    router.replace(`/orders/${created.id}`)
  }
}

async function addLine() {
  const qty = Number(lineQty.value)
  if (!lineProduct.value || !Number.isInteger(qty) || qty < 1) {
    notify('Produkt und Menge (≥ 1) angeben', 'error')
    return
  }
  const ok = await run(() => api.orders.addLine(current.value.id, lineProduct.value, qty))
  if (ok) {
    lineProduct.value = ''
    lineQty.value = 1
  }
}

const removeLine = (productId) => run(() => api.orders.removeLine(current.value.id, productId))
const place = () => run(() => api.orders.place(current.value.id), 'Bestellung aufgegeben')
const ship = () => run(() => api.orders.ship(current.value.id), 'Bestellung versendet')
const cancel = () => {
  if (window.confirm(`Bestellung ${current.value.number} stornieren?`)) {
    run(() => api.orders.cancel(current.value.id), 'Bestellung storniert')
  }
}

const isDraft = computed(() => current.value?.status === 'DRAFT')
const isPlaced = computed(() => current.value?.status === 'PLACED')
</script>

<template>
  <div class="page-head">
    <h1>Bestellungen</h1>
    <form class="toolbar" @submit.prevent="createOrder">
      <select v-model="newCustomer" aria-label="Kunde für neue Bestellung">
        <option value="">Kunde wählen …</option>
        <option v-for="c in activeCustomers" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <button class="btn btn-primary" :disabled="busy">+ Neue Bestellung</button>
    </form>
  </div>

  <div class="layout" :class="{ 'with-detail': current }">
    <section class="card">
      <div class="filters">
        <button class="chip" :class="{ on: !statusFilter }" @click="statusFilter = ''">Alle</button>
        <button v-for="(info, s) in STATUS" :key="s" class="chip" :class="{ on: statusFilter === s }" @click="statusFilter = s">
          {{ info.label }}
        </button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Nr.</th><th>Kunde</th><th>Status</th><th>Erfasst</th><th class="num">Total</th></tr>
          </thead>
          <tbody>
            <tr v-for="o in visible" :key="o.id" class="clickable" :class="{ selected: o.id === selectedId }" @click="select(o)">
              <td><code>{{ o.number }}</code></td>
              <td>{{ o.customerName }}</td>
              <td><StatusBadge :status="o.status" /></td>
              <td class="muted">{{ dateTime(o.createdAt) }}</td>
              <td class="num">{{ money(o.total, o.currency) }}</td>
            </tr>
            <tr v-if="!visible.length"><td colspan="5" class="empty">Keine Bestellungen</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="current" class="card card-pad detail">
      <header class="detail-head">
        <div>
          <h2>Bestellung {{ current.number }}</h2>
          <p class="muted">{{ current.customerName }}</p>
        </div>
        <button class="btn btn-ghost btn-icon" aria-label="Detail schliessen" @click="select(null)">✕</button>
      </header>

      <dl class="meta">
        <div><dt>Status</dt><dd><StatusBadge :status="current.status" /></dd></div>
        <div><dt>Erfasst</dt><dd>{{ dateTime(current.createdAt) }}</dd></div>
        <div v-if="current.placedAt"><dt>Aufgegeben</dt><dd>{{ dateTime(current.placedAt) }}</dd></div>
      </dl>

      <table class="lines">
        <thead>
          <tr><th>Produkt</th><th class="num">Menge</th><th class="num">Preis</th><th class="num">Total</th><th v-if="isDraft"></th></tr>
        </thead>
        <tbody>
          <tr v-for="l in current.lines" :key="l.productId">
            <td>{{ l.productName }}</td>
            <td class="num">{{ l.quantity }}</td>
            <td class="num">{{ money(l.unitPrice, current.currency) }}</td>
            <td class="num">{{ money(l.lineTotal, current.currency) }}</td>
            <td v-if="isDraft" class="num">
              <button class="btn btn-ghost btn-icon btn-danger" :disabled="busy" aria-label="Position entfernen" @click="removeLine(l.productId)">🗑</button>
            </td>
          </tr>
          <tr v-if="!current.lines.length"><td :colspan="isDraft ? 5 : 4" class="empty">Noch keine Positionen</td></tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="3"><strong>Total</strong></td>
            <td class="num"><strong>{{ money(current.total, current.currency) }}</strong></td>
            <td v-if="isDraft"></td>
          </tr>
        </tfoot>
      </table>

      <form v-if="isDraft" class="add-line" @submit.prevent="addLine">
        <select v-model="lineProduct" aria-label="Produkt">
          <option value="">Produkt wählen …</option>
          <option v-for="p in activeProducts" :key="p.id" :value="p.id">{{ p.name }} ({{ money(p.price, p.currency) }})</option>
        </select>
        <input v-model.number="lineQty" type="number" min="1" step="1" aria-label="Menge" class="qty" />
        <button class="btn" :disabled="busy">Hinzufügen</button>
      </form>

      <div v-if="isDraft || isPlaced" class="actions">
        <button v-if="isDraft" class="btn btn-primary" :disabled="busy" @click="place">✓ Aufgeben</button>
        <button v-if="isPlaced" class="btn btn-primary" :disabled="busy" @click="ship">Versenden</button>
        <button class="btn btn-danger" :disabled="busy" @click="cancel">Stornieren</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
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
.actions { display: flex; gap: 8px; padding-top: 4px; border-top: 1px solid var(--border); padding-top: 16px; }
</style>
