import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, clearAuth, loadAuth, setUnauthorizedHandler } from './api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => loadAuth()?.username ?? null)

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

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}
