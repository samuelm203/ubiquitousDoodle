import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Customers from './pages/Customers.jsx'
import Products from './pages/Products.jsx'
import Orders from './pages/Orders.jsx'

export default function App() {
  const { user, logout } = useAuth()

  if (!user) return <Login />

  return (
    <div className="shell">
      <header className="topbar">
        <strong className="brand">Orders</strong>
        <nav>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/orders">Bestellungen</NavLink>
          <NavLink to="/customers">Kunden</NavLink>
          <NavLink to="/products">Produkte</NavLink>
        </nav>
        <span className="spacer" />
        <span className="muted">{user}</span>
        <button className="btn ghost" onClick={logout}>Abmelden</button>
      </header>
      <main className="content">
        <Routes>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/customers" element={<Customers />} />
          <Route path="/products" element={<Products />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </main>
    </div>
  )
}
