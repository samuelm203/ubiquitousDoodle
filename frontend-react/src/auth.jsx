import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, clearAuth, loadAuth, setUnauthorizedHandler } from './api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => loadAuth()?.username ?? null)

  // Rolle ermitteln: Die Konfigurations-API ist nur für ADMIN lesbar (sonst 403)
  const [isAdmin, setIsAdmin] = useState(false)
  useEffect(() => {
    let alive = true
    setIsAdmin(false)
    if (user) api.propertyCompanies().then(() => alive && setIsAdmin(true), () => {})
    return () => { alive = false }
  }, [user])

  const logout = useCallback(() => {
    clearAuth()
    setUser(null)
  }, [])

  // Bei 401 irgendwo in der App: zurück zum Login
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const login = useCallback(async (username, password) => {
    await api.login(username, password)
    setUser(username)
  }, [])

  return <AuthContext.Provider value={{ user, isAdmin, login, logout }}>{children}</AuthContext.Provider>
}
