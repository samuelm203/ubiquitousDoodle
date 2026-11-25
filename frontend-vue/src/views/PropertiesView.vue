<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { api } from '../api.js'
import { attempt } from '../notify.js'

const companies = ref([])
const company = ref('')
const filter = ref('')
const onlyDifferences = ref(false)
const page = ref(0)
const size = ref(100)
const table = shallowRef({ columns: [], rows: [], total: 0 })
const loading = ref(false)

// Offene Änderungen über Seiten und Filter hinweg: "spalte\0schlüssel" -> { column, key, value } (value null = löschen).
// shallowRef (auch für die Tabelle) + neue Map pro Änderung: Vue verfolgt nicht jede Zelle einzeln.
const edits = shallowRef(new Map())
const pending = computed(() => edits.value.size)
const idOf = (column, key) => `${column}\0${key}`

const pages = computed(() => Math.max(1, Math.ceil(table.value.total / size.value)))

let seq = 0
async function load() {
  if (!company.value) return
  const mine = ++seq
  loading.value = true
  const result = await attempt(() =>
    api.properties.table(company.value, { filter: filter.value.trim(), onlyDifferences: onlyDifferences.value, page: page.value, size: size.value }),
  )
  if (mine !== seq) return // eine neuere Anfrage ist unterwegs
  loading.value = false
  if (result) table.value = result
}

onMounted(async () => {
  companies.value = (await attempt(api.properties.companies)) ?? []
  if (companies.value.length) company.value = companies.value[0].code
})

function reload() {
  if (page.value === 0) load()
  else page.value = 0 // der page-Watcher lädt
}

// Tippen im Filter: kurz warten, dann auf Seite 0 neu laden
let timer
watch(filter, () => {
  clearTimeout(timer)
  timer = setTimeout(reload, 300)
})
onBeforeUnmount(() => clearTimeout(timer))
watch([onlyDifferences, size], reload)
watch(page, load)

watch(company, (now, old) => {
  if (old && pending.value && !window.confirm(`${pending.value} ungespeicherte Änderung(en) verwerfen?`)) {
    company.value = old
    return
  }
  edits.value = new Map()
  reload()
})

function shown(row, i) {
  const e = edits.value.get(idOf(table.value.columns[i], row.key))
  return e ? e.value : row.values[i]
}

function setEdit(row, i, value) {
  const column = table.value.columns[i]
  const id = idOf(column, row.key)
  const next = new Map(edits.value)
  // Zurück auf den Originalwert (oder leer bei fehlendem Schlüssel) = keine Änderung
  if (value === row.values[i] || (row.values[i] === null && value === '')) next.delete(id)
  else next.set(id, { column, key: row.key, value })
  edits.value = next
}

function discard() {
  edits.value = new Map()
}

const saving = ref(false)
async function save() {
  saving.value = true
  const changes = [...edits.value.values()]
  const ok = await attempt(() => api.properties.change(company.value, changes).then(() => true), `${changes.length} Änderung(en) gespeichert`)
  saving.value = false
  if (ok) {
    edits.value = new Map()
    load()
  }
}

function cell(row, i) {
  const v = shown(row, i)
  return { value: v, missing: v === null, dirty: edits.value.has(idOf(table.value.columns[i], row.key)) }
}
</script>

<template>
  <div class="page-head">
    <h1>Firmenkonfiguration</h1>
    <div class="toolbar">
      <span v-if="pending" class="muted">{{ pending }} ungespeichert</span>
      <button class="btn" :disabled="!pending || saving" @click="discard">Verwerfen</button>
      <button class="btn btn-primary" :disabled="!pending || saving" @click="save">{{ saving ? 'Speichern …' : 'Speichern' }}</button>
    </div>
  </div>

  <div class="toolbar props-filters">
    <select v-model="company" aria-label="Firma">
      <option v-for="c in companies" :key="c.code" :value="c.code">{{ c.code }} ({{ c.keyCount }})</option>
    </select>
    <input v-model="filter" type="search" placeholder="Schlüssel filtern …" aria-label="Schlüssel filtern" />
    <label class="check"><input v-model="onlyDifferences" type="checkbox" /> Nur Unterschiede</label>
  </div>

  <div class="card table-wrap props-table" :class="{ busy: loading }">
    <table>
      <thead>
        <tr>
          <th>Schlüssel</th>
          <th v-for="c in table.columns" :key="c">{{ c }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in table.rows" :key="row.key">
          <td class="key">{{ row.key }}</td>
          <td v-for="(col, i) in table.columns" :key="col" class="cellbox">
            <input
              :value="cell(row, i).value ?? ''"
              :placeholder="cell(row, i).missing ? 'fehlt' : ''"
              :class="{ dirty: cell(row, i).dirty, missing: cell(row, i).missing }"
              :aria-label="`${row.key} ${col}`"
              @change="setEdit(row, i, $event.target.value)"
            />
            <button v-if="!cell(row, i).missing" class="btn btn-ghost btn-icon del" title="Schlüssel in dieser Spalte löschen" @click="setEdit(row, i, null)">×</button>
            <button v-else-if="cell(row, i).dirty" class="btn btn-ghost btn-icon del" title="Rückgängig" @click="setEdit(row, i, row.values[i])">↶</button>
          </td>
        </tr>
        <tr v-if="!table.rows.length"><td :colspan="table.columns.length + 1" class="empty">{{ loading ? 'Lädt …' : 'Keine Schlüssel gefunden' }}</td></tr>
      </tbody>
    </table>
  </div>

  <div class="toolbar pager">
    <button class="btn" :disabled="page === 0" @click="page--">‹ Zurück</button>
    <span class="muted">Seite {{ page + 1 }} / {{ pages }} · {{ table.total }} Schlüssel</span>
    <button class="btn" :disabled="page + 1 >= pages" @click="page++">Weiter ›</button>
    <select v-model.number="size" aria-label="Zeilen pro Seite">
      <option v-for="n in [50, 100, 200]" :key="n" :value="n">{{ n }} pro Seite</option>
    </select>
  </div>
</template>

<style scoped>
.props-filters { margin-bottom: 12px; }
.props-filters input[type='search'] { flex: 1; min-width: 200px; }
.props-table.busy { opacity: 0.6; }
.key { font-family: ui-monospace, monospace; font-size: 0.85rem; white-space: nowrap; }
th, td { padding: 4px 8px; }
.cellbox { white-space: nowrap; }
.cellbox input { width: calc(100% - 30px); min-width: 120px; padding: 4px 8px; font-size: 0.85rem; font-family: ui-monospace, monospace; }
.cellbox input.missing { background: color-mix(in srgb, var(--muted) 10%, transparent); }
.cellbox input.dirty { border-color: var(--primary); background: color-mix(in srgb, var(--primary) 9%, transparent); }
.del { padding: 2px 6px; }
.pager { margin-top: 12px; justify-content: center; }
</style>
