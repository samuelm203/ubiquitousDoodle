<script>
  import { auth } from './auth.svelte.js'
  import { api, setUnauthorizedHandler } from './api.js'
  import { route, navigate } from './router.svelte.js'
  import { toasts } from './toast.svelte.js'
  import Login from './views/Login.svelte'
  import Dashboard from './views/Dashboard.svelte'
  import Orders from './views/Orders.svelte'
  import Customers from './views/Customers.svelte'
  import Products from './views/Products.svelte'

  const nav = [
    { to: '/', label: 'Übersicht', icon: '◧' },
    { to: '/orders', label: 'Bestellungen', icon: '▤' },
    { to: '/customers', label: 'Kunden', icon: '◎' },
    { to: '/products', label: 'Produkte', icon: '▣' },
  ]

  const loggedIn = $derived(auth.header !== null)
  const orderId = $derived(route.path.startsWith('/orders/') ? route.path.slice('/orders/'.length) : null)

  function isActive(to) {
    return to === '/' ? route.path === '/' : route.path.startsWith(to)
  }

  function signOut() {
    api.logout()
    navigate('/login')
  }

  // Abgelaufene oder falsche Zugangsdaten -> zurück zum Login
  setUnauthorizedHandler(() => {
    api.logout()
    navigate('/login')
  })

  // Login-Guard
  $effect(() => {
    if (!loggedIn && route.path !== '/login') navigate('/login', { replace: true })
    else if (loggedIn && route.path === '/login') navigate('/', { replace: true })
  })

  $effect(() => {
    const titles = { '/': 'Übersicht', '/customers': 'Kunden', '/products': 'Produkte' }
    const t = route.path.startsWith('/orders') ? 'Bestellungen' : titles[route.path]
    document.title = t ? `${t} · Orders` : 'Orders'
  })
</script>

{#if !loggedIn}
  <Login />
{:else}
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">Orders</div>
      <nav>
        {#each nav as item (item.to)}
          <a href="#{item.to}" class="nav-link" class:active={isActive(item.to)}>
            <span class="nav-icon" aria-hidden="true">{item.icon}</span>{item.label}
          </a>
        {/each}
      </nav>
      <div class="sidebar-foot">
        <span class="muted">Angemeldet als <strong>{auth.username}</strong></span>
        <button class="btn btn-ghost" onclick={signOut}>Abmelden</button>
      </div>
    </aside>
    <main class="content">
      {#if route.path.startsWith('/orders')}
        <Orders selectedId={orderId} />
      {:else if route.path === '/customers'}
        <Customers />
      {:else if route.path === '/products'}
        <Products />
      {:else}
        <Dashboard />
      {/if}
    </main>
  </div>
{/if}

<div class="toasts" aria-live="polite">
  {#each toasts as t (t.id)}
    <div class="toast toast-{t.kind}">{t.message}</div>
  {/each}
</div>
