<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api.js'
import { setCredentials, setAdmin, logout } from '../auth.js'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  setCredentials(username.value.trim(), password.value)
  try {
    // HTTP Basic hat kein Login-Endpoint: ein geschützter GET prüft die Zugangsdaten
    await api.dashboard()
    // Rolle erkennen: Firmenkonfiguration ist nur für ADMIN lesbar (sonst 403)
    setAdmin(await api.properties.companies().then(() => true, (e) => (e.status === 401 ? Promise.reject(e) : false)))
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (e) {
    logout()
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="card login-card" @submit.prevent="submit">
      <h1>Orders</h1>
      <p class="muted">Bitte anmelden</p>
      <label class="field">
        <span>Benutzername</span>
        <input v-model="username" autocomplete="username" required autofocus />
      </label>
      <label class="field">
        <span>Passwort</span>
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="error" class="login-error" role="alert">{{ error }}</p>
      <button class="btn btn-primary login-btn" :disabled="busy">{{ busy ? 'Anmelden …' : 'Anmelden' }}</button>
      <p class="muted hint">Demo: <code>admin / admin</code> oder <code>user / user</code></p>
    </form>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 16px; }
.login-card { width: min(360px, 100%); padding: 28px; display: grid; gap: 14px; }
.login-card p { margin: 0; }
.login-btn { justify-content: center; padding: 9px; }
.login-error { color: var(--danger); font-size: 0.9rem; }
.hint { font-size: 0.85rem; text-align: center; }
</style>
