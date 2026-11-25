<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api.js'
import { attempt } from '../notify.js'
import ModalDialog from '../components/ModalDialog.vue'

const customers = ref([])
const filter = ref('')
const editing = ref(null) // null = geschlossen, {} = neu, {id,...} = bearbeiten
const form = reactive({ name: '', email: '', city: '', active: true })

async function load() {
  customers.value = (await attempt(api.customers.list)) ?? []
}
onMounted(load)

const visible = computed(() => {
  const q = filter.value.trim().toLowerCase()
  const list = customers.value.slice().sort((a, b) => a.name.localeCompare(b.name, 'de'))
  if (!q) return list
  return list.filter((c) => [c.name, c.email, c.city].some((v) => v?.toLowerCase().includes(q)))
})

function open(customer) {
  editing.value = customer ?? {}
  Object.assign(form, customer
    ? { name: customer.name, email: customer.email, city: customer.city ?? '', active: customer.active }
    : { name: '', email: '', city: '', active: true })
}

async function save() {
  const id = editing.value.id
  const body = { ...form, city: form.city || null }
  const ok = await attempt(
    () => (id ? api.customers.update(id, body) : api.customers.create(body)),
    id ? 'Kunde gespeichert' : 'Kunde angelegt',
  )
  if (ok) {
    editing.value = null
    load()
  }
}

async function remove() {
  const c = editing.value
  if (!window.confirm(`Kunde «${c.name}» wirklich löschen?`)) return
  const ok = await attempt(() => api.customers.remove(c.id).then(() => true), 'Kunde gelöscht')
  if (ok) {
    editing.value = null
    load()
  }
}
</script>

<template>
  <div class="page-head">
    <h1>Kunden</h1>
    <div class="toolbar">
      <input v-model="filter" type="search" placeholder="Suchen …" aria-label="Kunden suchen" />
      <button class="btn btn-primary" @click="open()">+ Neuer Kunde</button>
    </div>
  </div>

  <div class="card table-wrap">
    <table>
      <thead>
        <tr><th>Name</th><th>E-Mail</th><th>Ort</th><th>Status</th></tr>
      </thead>
      <tbody>
        <tr v-for="c in visible" :key="c.id" class="clickable" :class="{ inactive: !c.active }" @click="open(c)">
          <td><strong>{{ c.name }}</strong></td>
          <td>{{ c.email }}</td>
          <td>{{ c.city }}</td>
          <td><span class="badge" :class="c.active ? 'badge-success' : 'badge-neutral'">{{ c.active ? 'Aktiv' : 'Inaktiv' }}</span></td>
        </tr>
        <tr v-if="!visible.length"><td colspan="4" class="empty">Keine Kunden gefunden</td></tr>
      </tbody>
    </table>
  </div>

  <ModalDialog v-if="editing" :title="editing.id ? 'Kunde bearbeiten' : 'Neuer Kunde'" @close="editing = null">
    <form class="form-grid" @submit.prevent="save">
      <label class="field"><span>Name</span><input v-model="form.name" required autofocus /></label>
      <label class="field"><span>E-Mail</span><input v-model="form.email" type="email" required /></label>
      <label class="field"><span>Ort</span><input v-model="form.city" /></label>
      <label class="check"><input v-model="form.active" type="checkbox" /> Aktiv</label>
      <div class="form-actions">
        <button v-if="editing.id" type="button" class="btn btn-danger" style="margin-right: auto" @click="remove">Löschen</button>
        <button type="button" class="btn" @click="editing = null">Abbrechen</button>
        <button class="btn btn-primary">Speichern</button>
      </div>
    </form>
  </ModalDialog>
</template>
