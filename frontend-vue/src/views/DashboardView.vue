<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api.js'
import { attempt } from '../notify.js'
import { money, STATUS } from '../format.js'
import StatusBadge from '../components/StatusBadge.vue'

const data = ref(null)
const recent = ref([])

onMounted(async () => {
  const [d, orders] = await Promise.all([attempt(api.dashboard), attempt(api.orders.list)])
  data.value = d ?? null
  recent.value = (orders ?? [])
    .slice()
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''))
    .slice(0, 5)
})

const byStatus = (s) => data.value?.ordersByStatus?.[s] ?? 0
const totalOrders = computed(() => Object.keys(STATUS).reduce((sum, s) => sum + byStatus(s), 0))
</script>

<template>
  <div class="page-head">
    <h1>Übersicht</h1>
  </div>

  <template v-if="data">
    <div class="tiles">
      <div class="card card-pad tile tile-wide">
        <span class="muted">Umsatz (aufgegeben + versendet)</span>
        <strong>{{ money(data.revenue, data.currency) }}</strong>
      </div>
      <div class="card card-pad tile">
        <span class="muted">Offene Bestellungen</span>
        <strong>{{ byStatus('PLACED') }}</strong>
      </div>
      <div class="card card-pad tile">
        <span class="muted">Kunden</span>
        <strong>{{ data.customers }}</strong>
      </div>
      <div class="card card-pad tile">
        <span class="muted">Produkte</span>
        <strong>{{ data.products }}</strong>
      </div>
    </div>

    <div class="grid2">
      <section class="card card-pad">
        <h2>Bestellungen nach Status</h2>
        <ul class="bars">
          <li v-for="(info, s) in STATUS" :key="s">
            <StatusBadge :status="s" />
            <div class="bar"><div class="bar-fill" :class="`fill-${info.tone}`" :style="{ width: totalOrders ? (byStatus(s) / totalOrders) * 100 + '%' : '0' }" /></div>
            <span class="num">{{ byStatus(s) }}</span>
          </li>
        </ul>
      </section>

      <section class="card">
        <div class="card-pad section-head">
          <h2>Neueste Bestellungen</h2>
          <RouterLink to="/orders" class="muted">Alle anzeigen →</RouterLink>
        </div>
        <table>
          <tbody>
            <tr v-for="o in recent" :key="o.id" class="clickable" @click="$router.push(`/orders/${o.id}`)">
              <td><strong>{{ o.number }}</strong><br /><span class="muted">{{ o.customerName }}</span></td>
              <td><StatusBadge :status="o.status" /></td>
              <td class="num">{{ money(o.total, o.currency) }}</td>
            </tr>
            <tr v-if="!recent.length"><td class="empty" colspan="3">Noch keine Bestellungen</td></tr>
          </tbody>
        </table>
      </section>
    </div>
  </template>
  <p v-else class="muted">Lade …</p>
</template>

<style scoped>
.tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 18px; }
.tile { display: flex; flex-direction: column; gap: 6px; }
.tile span { font-size: 0.9rem; }
.tile strong { font-size: 1.8rem; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }
.tile-wide { grid-column: span 2; }
@media (max-width: 760px) { .tile-wide { grid-column: auto; } }
.grid2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 14px; }
.section-head { display: flex; justify-content: space-between; align-items: center; padding-bottom: 6px; }
.section-head a { font-size: 0.9rem; text-decoration: none; }
.bars { list-style: none; padding: 0; margin: 16px 0 0; display: grid; gap: 12px; }
.bars li { display: grid; grid-template-columns: 100px 1fr 32px; align-items: center; gap: 12px; }
.bar { height: 8px; border-radius: 4px; background: var(--bg); overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
.fill-neutral { background: var(--muted); }
.fill-info { background: var(--info); }
.fill-success { background: var(--success); }
.fill-danger { background: var(--danger); }
</style>
