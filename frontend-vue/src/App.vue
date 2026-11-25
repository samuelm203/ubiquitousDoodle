<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, logout } from './auth.js'
import ToastStack from './components/ToastStack.vue'

const route = useRoute()
const router = useRouter()
const showShell = computed(() => !route.meta.public)

const nav = [
  { to: '/', label: 'Übersicht', icon: '◧' },
  { to: '/orders', label: 'Bestellungen', icon: '▤' },
  { to: '/customers', label: 'Kunden', icon: '◎' },
  { to: '/products', label: 'Produkte', icon: '▣' },
]

function isActive(to) {
  return to === '/' ? route.path === '/' : route.path.startsWith(to)
}

function signOut() {
  logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div v-if="showShell" class="shell">
    <aside class="sidebar">
      <div class="brand">Orders</div>
      <nav>
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="nav-link" :class="{ active: isActive(item.to) }">
          <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
        </RouterLink>
      </nav>
      <div class="sidebar-foot">
        <span class="muted">Angemeldet als <strong>{{ auth.username }}</strong></span>
        <button class="btn btn-ghost" @click="signOut">Abmelden</button>
      </div>
    </aside>
    <main class="content">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
  <ToastStack />
</template>
