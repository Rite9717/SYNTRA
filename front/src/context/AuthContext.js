import { createContext, useState, useContext, useEffect } from 'react'
import authService from '../services/authService'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = sessionStorage.getItem('user')
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    const response = await authService.login(username, password)
    setUser(response)
    sessionStorage.setItem('user', JSON.stringify(response))
    sessionStorage.setItem('token', response.token)
    return response
  }

  const signup = async (userData) => {
    return await authService.signup(userData)
  }

  const logout = () => {
    setUser(null)
    sessionStorage.removeItem('user')
    sessionStorage.removeItem('token')
  }

  if (loading) {
    return <div>Loading...</div>
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
