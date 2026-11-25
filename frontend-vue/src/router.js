import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './auth.js'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
    { path: '/', name: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { title: 'Übersicht' } },
    { path: '/orders/:id?', name: 'orders', component: () => import('./views/OrdersView.vue'), meta: { title: 'Bestellungen' } },
    { path: '/customers', name: 'customers', component: () => import('./views/CustomersView.vue'), meta: { title: 'Kunden' } },
    { path: '/products', name: 'products', component: () => import('./views/ProductsView.vue'), meta: { title: 'Produkte' } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !auth.loggedIn) {
    return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  }
  if (to.name === 'login' && auth.loggedIn) return { name: 'dashboard' }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Orders` : 'Orders'
})
