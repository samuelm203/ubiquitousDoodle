<script>
  import { api } from '../api.js'
  import { navigate } from '../router.svelte.js'

  let username = $state('')
  let password = $state('')
  let error = $state('')
  let busy = $state(false)

  async function submit(e) {
    e.preventDefault()
    error = ''
    busy = true
    try {
      await api.login(username.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      error = err.message
    } finally {
      busy = false
    }
  }
</script>

<div class="login-page">
  <form class="card login-card" onsubmit={submit}>
    <h1>Orders</h1>
    <p class="muted">Bitte anmelden</p>
    <label class="field">
      <span>Benutzername</span>
      <input bind:value={username} autocomplete="username" required />
    </label>
    <label class="field">
      <span>Passwort</span>
      <input bind:value={password} type="password" autocomplete="current-password" required />
    </label>
    {#if error}<p class="login-error" role="alert">{error}</p>{/if}
    <button class="btn btn-primary login-btn" disabled={busy}>{busy ? 'Anmelden …' : 'Anmelden'}</button>
    <p class="muted hint">Demo: <code>admin / admin</code> oder <code>user / user</code></p>
  </form>
</div>

<style>
  .login-page { min-height: 100vh; display: grid; place-items: center; padding: 16px; }
  .login-card { width: min(360px, 100%); padding: 28px; display: grid; gap: 14px; }
  .login-card p { margin: 0; }
  .login-btn { justify-content: center; padding: 9px; }
  .login-error { color: var(--danger); font-size: 0.9rem; }
  .hint { font-size: 0.85rem; text-align: center; }
</style>
