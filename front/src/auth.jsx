import { createContext, useContext, useMemo, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = sessionStorage.getItem('syntra_user')
    return raw ? JSON.parse(raw) : null
  })

  const value = useMemo(() => ({
    user,
    login(data) {
      sessionStorage.setItem('syntra_token', data.token)
      sessionStorage.setItem('syntra_user', JSON.stringify(data))
      setUser(data)
    },
    logout() {
      sessionStorage.clear()
      setUser(null)
    },
    isAdmin: user?.roles?.includes('ROLE_ADMIN')
  }), [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
